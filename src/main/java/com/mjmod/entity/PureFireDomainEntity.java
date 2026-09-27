package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModParticles;
import com.mjmod.skill.PureFireDomainGeometry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkHooks;

import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 纯质阳炎十技能【阳炎领域】的服务端权威逻辑锚点。
 *
 * 领域中心在释放瞬间固定，水平边长 150 格（半宽 75，高度向下 3 格到向上 57 格），
 * 生命周期 1200 ticks；客户端只负责读取实体的位置并绘制结界描边、辉光
 * 与蔓延动画，避免把昂贵的方块扫描放进实体渲染器。
 */
public final class PureFireDomainEntity extends Entity {
    public static final int DURATION_TICKS = 1200;
    /** 几何与描边共享边界；AABB 的上界不包含端点。 */
    public static final double HORIZONTAL_HALF_SIZE = PureFireDomainGeometry.HALF_SIZE;
    public static final int MIN_Y_OFFSET = PureFireDomainGeometry.MIN_Y;
    public static final int MAX_Y_OFFSET = PureFireDomainGeometry.MAX_Y;
    public static final double BOUNDARY_RADIUS = HORIZONTAL_HALF_SIZE;
    public static final int DRAIN_INTERVAL_TICKS = 200;
    private static final double DRAIN_RATIO = 0.02D;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireDomainEntity.class, EntityDataSerializers.INT);

    private final Map<UUID, TargetState> targetStates = new HashMap<>();
    private UUID ownerUuid;
    private UUID ownerModifierId;
    private UUID handModifierId;
    private double ownerAttackStolen;

    private static final class TargetState {
        private LivingEntity entity;
        private int dwellTicks;
        private int drainStacks;
    }

    public PureFireDomainEntity(EntityType<? extends PureFireDomainEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireDomainEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_DOMAIN.get(), level);
        ownerUuid = owner.getUUID();
        entityData.set(OWNER_ID, owner.getId());
        // 偶数边长以整数方块边界为轴，确保扫描与效果 AABB 恰好都是 150 列。
        setPos(PureFireDomainGeometry.anchor(owner.getX()), Math.floor(owner.getY()),
                PureFireDomainGeometry.anchor(owner.getZ()));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
    }

    public LivingEntity getOwner() {
        // 实体运行时 ID 在存档重载后会改变；服务端始终以持久 UUID 查找。
        Entity entity = level() instanceof ServerLevel serverLevel
                ? (ownerUuid == null ? null : serverLevel.getEntity(ownerUuid))
                : level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    public int getRemainingTicks() {
        return Math.max(0, DURATION_TICKS - tickCount);
    }

    public AABB getDomainBounds() {
        return new AABB(
                getX() - HORIZONTAL_HALF_SIZE, getY() + MIN_Y_OFFSET, getZ() - HORIZONTAL_HALF_SIZE,
                getX() + HORIZONTAL_HALF_SIZE, getY() + MAX_Y_OFFSET, getZ() + HORIZONTAL_HALF_SIZE);
    }

    public boolean contains(LivingEntity entity) {
        return getDomainBounds().contains(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        LivingEntity owner = getOwner();
        if (!(owner instanceof ServerPlayer serverOwner) || !owner.isAlive()) {
            cleanupModifiers();
            discard();
            return;
        }
        if (entityData.get(OWNER_ID) != serverOwner.getId()) {
            entityData.set(OWNER_ID, serverOwner.getId());
        }
        if (tickCount >= DURATION_TICKS) {
            cleanupModifiers();
            // 领域到期消散音效
            if (level() instanceof ServerLevel serverLevelExpire) {
                serverLevelExpire.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 0.9F, 0.9F);
            }
            discard();
            return;
        }

        // 开启音效 + 最后 10 秒倒计时滴答
        if (tickCount == 1) {
            level().playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.9F, 0.8F);
        } else {
            int remaining = DURATION_TICKS - tickCount;
            if (remaining <= 200 && remaining % 40 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(),
                        SoundSource.PLAYERS, 0.7F, 1.0F + (200 - remaining) / 400.0F);
            }
        }

        if (contains(serverOwner)) {
            refreshOwnerEffects(serverOwner);
        }
        refreshHandFormDamage(serverOwner);
        applyDomainEffects(serverOwner);

        if (tickCount % 10 == 0) {
            spawnDomainParticles(serverOwner);
        }
    }

    private void refreshOwnerEffects(ServerPlayer owner) {
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 1, false, true, true));
        owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0, false, true, true));
    }

    /** 附焰拳脚属于纯火近战：属性乘区只作用于真实近战，不重复放大技能实体伤害。 */
    private void refreshHandFormDamage(ServerPlayer owner) {
        AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        if (handModifierId == null) handModifierId = ownerModifierId(owner.getUUID(), "hand_form");
        if (contains(owner) && owner.hasEffect(ModEffects.PURE_FIRE_HAND_FORM.get())) {
            if (attack.getModifier(handModifierId) == null) {
                attack.addTransientModifier(new AttributeModifier(handModifierId,
                        "阳炎领域·附焰近战增伤", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        } else {
            attack.removeModifier(handModifierId);
        }
    }

    private void applyDomainEffects(ServerPlayer owner) {
        AABB bounds = getDomainBounds();
        Map<UUID, LivingEntity> inside = new HashMap<>();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity.isAlive() && entity != owner && contains(entity))) {
            inside.put(target.getUUID(), target);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, false, true, true));
            if (tickCount % 5 == 0 && level() instanceof ServerLevel serverLevel) {
                double headY = target.getY() + target.getBbHeight() + 0.18D;
                // 压制黑火漩涡：暗红灵力微粒绕头顶旋转下压
                double swirl = tickCount * 0.22D;
                for (int i = 0; i < 3; i++) {
                    double angle = swirl + i * Math.PI * 2.0D / 3.0D;
                    serverLevel.sendParticles(
                            new DustParticleOptions(new Vector3f(0.36F, 0.02F, 0.02F), 1.1F),
                            target.getX() + Math.cos(angle) * 0.34D,
                            headY + 0.06D + Math.sin(angle * 2.0D) * 0.05D,
                            target.getZ() + Math.sin(angle) * 0.34D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), headY, target.getZ(),
                        1, 0.12D, 0.03D, 0.12D, 0.008D);
            }

            TargetState state = targetStates.computeIfAbsent(target.getUUID(), ignored -> new TargetState());
            state.entity = target;
            state.dwellTicks++;
            if (state.dwellTicks >= DRAIN_INTERVAL_TICKS) {
                state.dwellTicks -= DRAIN_INTERVAL_TICKS;
                drainAttackDamage(owner, target, state);
            }
        }

        Iterator<Map.Entry<UUID, TargetState>> iterator = targetStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TargetState> entry = iterator.next();
            if (!inside.containsKey(entry.getKey())) {
                removeTargetModifier(entry.getValue().entity, entry.getValue());
                iterator.remove();
            }
        }
    }

    private void drainAttackDamage(ServerPlayer owner, LivingEntity target, TargetState state) {
        AttributeInstance targetAttack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (targetAttack == null) return;

        double stolen = Math.max(0.0D, targetAttack.getValue() * DRAIN_RATIO);
        state.drainStacks++;
        UUID modifierId = targetModifierId(target.getUUID());
        targetAttack.removeModifier(modifierId);
        double remainingRatio = Math.pow(1.0D - DRAIN_RATIO, state.drainStacks);
        targetAttack.addTransientModifier(new AttributeModifier(
                modifierId, "阳炎领域·攻击力抽取", remainingRatio - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL));

        ownerAttackStolen += stolen;
        ownerModifierId = ownerModifierId(owner.getUUID());
        AttributeInstance ownerAttack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        if (ownerAttack != null) {
            ownerAttack.removeModifier(ownerModifierId);
            ownerAttack.addTransientModifier(new AttributeModifier(
                    ownerModifierId, "阳炎领域·吸收攻击力", ownerAttackStolen,
                    AttributeModifier.Operation.ADDITION));
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.PURE_FIRE.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.7D, target.getZ(),
                    8, 0.18D, 0.22D, 0.18D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                    6, 0.25D, 0.35D, 0.25D, 0.08D);
            level().playSound(null, owner.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.45F, 1.35F);
        }
    }

    private void removeTargetModifier(LivingEntity target, TargetState state) {
        if (target == null) return;
        AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.removeModifier(targetModifierId(target.getUUID()));
    }

    private void cleanupModifiers() {
        for (Map.Entry<UUID, TargetState> entry : targetStates.entrySet()) {
            removeTargetModifier(entry.getValue().entity, entry.getValue());
        }
        LivingEntity owner = getOwner();
        if (owner != null && ownerModifierId != null) {
            AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.removeModifier(ownerModifierId);
        }
        if (owner != null && handModifierId != null) {
            AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.removeModifier(handModifierId);
        }
        targetStates.clear();
        ownerAttackStolen = 0.0D;
        ownerModifierId = null;
        handModifierId = null;
    }

    private UUID targetModifierId(UUID target) {
        return UUID.nameUUIDFromBytes(("mjmod:pure_fire_domain_target:" + getUUID() + ":" + target)
                .getBytes(StandardCharsets.UTF_8));
    }

    private UUID ownerModifierId(UUID owner) {
        return UUID.nameUUIDFromBytes(("mjmod:pure_fire_domain_owner:" + getUUID() + ":" + owner)
                .getBytes(StandardCharsets.UTF_8));
    }

    private UUID ownerModifierId(UUID owner, String purpose) {
        return UUID.nameUUIDFromBytes(("mjmod:pure_fire_domain_owner:" + getUUID() + ":" + owner + ":" + purpose)
                .getBytes(StandardCharsets.UTF_8));
    }

    private void spawnDomainParticles(ServerPlayer owner) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        // 火粒子沿领域正方形边界巡游，勾勒 150×150 描边
        double drift = tickCount * 0.006D;
        double perimeter = 8.0D * HORIZONTAL_HALF_SIZE;
        for (int i = 0; i < 6; i++) {
            double t = drift + i / 6.0D;
            double p = (t - Math.floor(t)) * perimeter;
            double x;
            double z;
            if (p < 2.0D * HORIZONTAL_HALF_SIZE) {
                x = -HORIZONTAL_HALF_SIZE + p;
                z = -HORIZONTAL_HALF_SIZE;
            } else if (p < 4.0D * HORIZONTAL_HALF_SIZE) {
                x = HORIZONTAL_HALF_SIZE;
                z = -HORIZONTAL_HALF_SIZE + (p - 2.0D * HORIZONTAL_HALF_SIZE);
            } else if (p < 6.0D * HORIZONTAL_HALF_SIZE) {
                x = HORIZONTAL_HALF_SIZE - (p - 4.0D * HORIZONTAL_HALF_SIZE);
                z = HORIZONTAL_HALF_SIZE;
            } else {
                x = -HORIZONTAL_HALF_SIZE;
                z = HORIZONTAL_HALF_SIZE - (p - 6.0D * HORIZONTAL_HALF_SIZE);
            }
            serverLevel.sendParticles(ModParticles.PURE_FIRE.get(),
                    getX() + x,
                    getY() + 0.08D + ((tickCount + i * 5) % 14) * 0.06D,
                    getZ() + z,
                    1, 0.02D, 0.06D, 0.02D, 0.012D);
        }
        // 地表升腾神火碎屑：随机位置自地表向上蒸发
        for (int i = 0; i < 2; i++) {
            double ex = (level().random.nextDouble() * 2.0D - 1.0D) * HORIZONTAL_HALF_SIZE;
            double ez = (level().random.nextDouble() * 2.0D - 1.0D) * HORIZONTAL_HALF_SIZE;
            int bx = (int) Math.round(getX() + ex);
            int bz = (int) Math.round(getZ() + ez);
            if (!level().hasChunkAt(new net.minecraft.core.BlockPos(bx, (int) getY(), bz))) continue;
            double gy = level().getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.1D;
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                    getX() + ex, gy, getZ() + ez,
                    1, 0.04D, 0.12D, 0.04D, 0.012D);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) cleanupModifiers();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) ownerUuid = tag.getUUID("OwnerUUID");
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("DomainAge")) tickCount = tag.getInt("DomainAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("OwnerUUID", ownerUuid);
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("DomainAge", tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
