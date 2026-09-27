package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 纯质阳炎八技能【火墙】（Pure Fire Wall）实体控制器：
 * 1. 产生时机：施法后 8 ticks（0.4s）正式升起生成在施法者前方地表。
 * 2. 持续时间：100 ticks（5.0s）稳定驻留 + 20 ticks（1.0s）高度下沉与余烬熄灭消散动画，总寿命 120 ticks。
 * 3. 尺寸与形态：宽 6.0 格，高 2.8 格，厚度判定 1.8 格。由纵向升腾的线性粒子流构成。
 * 4. 伤害规则：靠近火墙（进入碰撞区域）即造成 8.0 点纯阳伤害 + 3 秒点燃。
 *    每个目标受击冷却 15 ticks（0.75s），保证持续封路威慑力同时防止每 tick 瞬间击杀。
 */
public final class PureFireWallEntity extends Entity {
    public static final int ACTIVE_TICKS = 100; // 稳定燃烧 5.0 秒
    public static final int FADE_TICKS = 20;    // 消散下沉 1.0 秒
    public static final int TOTAL_DURATION_TICKS = ACTIVE_TICKS + FADE_TICKS;

    public static final float WALL_WIDTH = 6.0F;
    public static final float WALL_HEIGHT = 2.8F;
    public static final float WALL_DEPTH = 1.8F;
    public static final float BASE_DAMAGE = 8.0F;
    public static final int HIT_COOLDOWN_TICKS = 15;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireWallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireWallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireWallEntity.class, EntityDataSerializers.FLOAT);

    /** 记录被击中生物的 ID 及冷却结束的 tickCount */
    private final Map<Integer, Integer> targetHitCooldowns = new HashMap<>();

    public PureFireWallEntity(EntityType<? extends PureFireWallEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireWallEntity(ServerLevel level, LivingEntity owner, Vec3 spawnPos, Vec3 direction) {
        this(ModEntities.PURE_FIRE_WALL.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        Vec3 horizDir = new Vec3(direction.x, 0, direction.z).normalize();
        if (horizDir.lengthSqr() < 1.0E-5D) horizDir = new Vec3(0, 0, 1);
        entityData.set(DIR_X, (float) horizDir.x);
        entityData.set(DIR_Z, (float) horizDir.z);
        setPos(spawnPos.x, spawnPos.y, spawnPos.z);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Z, 1.0F);
    }

    public LivingEntity getOwner() {
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    public Vec3 getWallNormal() {
        return new Vec3(entityData.get(DIR_X), 0.0D, entityData.get(DIR_Z)).normalize();
    }

    public Vec3 getWallRight() {
        Vec3 normal = getWallNormal();
        return new Vec3(-normal.z, 0.0D, normal.x).normalize();
    }

    /**
     * 计算当前火墙的有效高度因子（0.0 ~ 1.0）：
     * 前 6 ticks 拔地而起（0.0 -> 1.0）
     * 中间平稳保持（1.0）
     * 最后 20 ticks 渐进坍缩熄灭（1.0 -> 0.0）
     */
    public float getWallHeightFactor() {
        if (tickCount < 6) {
            return (tickCount + 1) / 6.0F;
        } else if (tickCount <= ACTIVE_TICKS) {
            return 1.0F;
        } else {
            float fadeProgress = (tickCount - ACTIVE_TICKS) / (float) FADE_TICKS;
            return Math.max(0.0F, 1.0F - fadeProgress);
        }
    }

    /** 是否处于消散阶段 */
    public boolean isFading() {
        return tickCount > ACTIVE_TICKS;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnClientWallParticles();
            return;
        }

        // 服务端逻辑
        if (tickCount == 1) {
            // 出墙初爆声
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.8F);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.6F);
        } else if (tickCount == ACTIVE_TICKS + 1) {
            // 开始消散熄灭声
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.7F);
        }

        if (tickCount >= TOTAL_DURATION_TICKS) {
            discard();
            return;
        }

        // 仅在非消散后期（高因子 > 0.15）且拥有伤害判定时对靠近目标结算
        float heightFactor = getWallHeightFactor();
        if (heightFactor > 0.15F) {
            checkWallCollisionsAndDamage(heightFactor);
        }
    }

    private void checkWallCollisionsAndDamage(float heightFactor) {
        LivingEntity owner = getOwner();
        Vec3 pos = position();
        Vec3 normal = getWallNormal();
        Vec3 right = getWallRight();

        float effectiveHeight = WALL_HEIGHT * heightFactor;
        AABB searchBox = new AABB(
                pos.x - WALL_WIDTH, pos.y - 0.5D, pos.z - WALL_WIDTH,
                pos.x + WALL_WIDTH, pos.y + effectiveHeight + 0.8D, pos.z + WALL_WIDTH
        );

        List<LivingEntity> nearby = level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != owner && (owner == null || !e.isAlliedTo(owner)));

        DamageSource source = owner instanceof ServerPlayer sp
                ? level().damageSources().playerAttack(sp)
                : level().damageSources().magic();

        for (LivingEntity target : nearby) {
            int targetId = target.getId();
            Integer nextAllowedTick = targetHitCooldowns.get(targetId);
            if (nextAllowedTick != null && tickCount < nextAllowedTick) {
                continue;
            }

            // 计算目标相对于火墙中心在法线（前后厚度）、切线（左右横向）、纵向（高度）的距离
            Vec3 offset = target.position().add(0, target.getBbHeight() * 0.5D, 0).subtract(pos);
            double depth = Math.abs(offset.dot(normal));
            double lateral = Math.abs(offset.dot(right));
            double yDiff = offset.y;

            // 厚度判定 1.8 格（前后各 0.9 格），横向左右各 3.1 格，高度 0 到 effectiveHeight
            if (depth <= (WALL_DEPTH * 0.5D + target.getBbWidth() * 0.4D)
                    && lateral <= (WALL_WIDTH * 0.5D + target.getBbWidth() * 0.3D)
                    && yDiff >= -0.2D && yDiff <= (effectiveHeight + 0.5D)) {

                target.hurt(source, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, BASE_DAMAGE));
                target.setSecondsOnFire(3);
                targetHitCooldowns.put(targetId, tickCount + HIT_COOLDOWN_TICKS);

                // 命中火花与燃爆声
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ModParticles.PURE_FIRE.get(),
                            target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            10, 0.2D, 0.25D, 0.2D, 0.05D);
                    sl.sendParticles(ParticleTypes.LAVA,
                            target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                            3, 0.15D, 0.15D, 0.15D, 0.02D);
                }
                level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 0.8F, 1.3F);
            }
        }
    }

    /** 客户端线性粒子渲染逻辑 */
    private void spawnClientWallParticles() {
        float heightFactor = getWallHeightFactor();
        if (heightFactor <= 0.02F) return;

        Vec3 pos = position();
        Vec3 normal = getWallNormal();
        Vec3 right = getWallRight();
        float currentHeight = WALL_HEIGHT * heightFactor;
        boolean fading = isFading();

        // 横向布设垂直线性火柱流（24 条纵向流）
        int streamCount = fading ? 14 : 24;
        double halfWidth = WALL_WIDTH * 0.5D;

        for (int i = 0; i < streamCount; i++) {
            double lateralOffset = -halfWidth + (i * (WALL_WIDTH / (streamCount - 1)));
            // 细微热浪起伏正弦波动
            double wavePhase = (tickCount * 0.25D) + (i * 0.45D);
            double depthJitter = Math.sin(wavePhase) * 0.08D;

            // 根基点
            Vec3 columnBase = pos.add(right.scale(lateralOffset)).add(normal.scale(depthJitter));

            // 每条线在垂直高度上分布 3~4 个向上流动的纯阳粒子
            int particlesPerStream = fading ? 2 : 4;
            for (int p = 0; p < particlesPerStream; p++) {
                double relY = (p / (double) particlesPerStream) * currentHeight + (random.nextDouble() * 0.35D);
                if (relY > currentHeight) continue;

                Vec3 particlePos = columnBase.add(0, relY, 0);

                // 垂直向上升腾的线性速度
                double upwardSpeed = fading ? 0.08D : 0.16D + random.nextDouble() * 0.06D;
                double sideDrift = (random.nextDouble() - 0.5D) * (fading ? 0.08D : 0.02D);

                level().addParticle(ModParticles.PURE_FIRE.get(),
                        particlePos.x, particlePos.y, particlePos.z,
                        right.x * sideDrift, upwardSpeed, right.z * sideDrift);
            }

            // 底部地火连线
            if (!fading && random.nextFloat() < 0.6F) {
                level().addParticle(ModParticles.PURE_FIRE.get(),
                        columnBase.x, columnBase.y + 0.05D, columnBase.z,
                        0.0D, 0.04D, 0.0D);
            }

            // 消散阶段释放熄灭余烬微烟
            if (fading && random.nextFloat() < 0.35F) {
                level().addParticle(ParticleTypes.SMOKE,
                        columnBase.x, columnBase.y + currentHeight * 0.8D, columnBase.z,
                        (random.nextDouble() - 0.5D) * 0.05D, 0.08D, (random.nextDouble() - 0.5D) * 0.05D);
            }
        }
    }

    /**
     * 辅助静态方法：在玩家正前方检测贴地坐标
     */
    public static Vec3 findGroundSpawnPos(Level level, LivingEntity caster, double forwardDistance) {
        Vec3 look = caster.getViewVector(1.0F);
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        if (horizLook.lengthSqr() < 1.0E-5D) horizLook = new Vec3(0, 0, 1);

        Vec3 target = caster.position().add(horizLook.scale(forwardDistance));
        // 从目标上方 2.5 格垂直向下射线检测坚实地面
        Vec3 rayStart = target.add(0, 2.5D, 0);
        Vec3 rayEnd = target.subtract(0, 3.5D, 0);
        BlockHitResult hit = level.clip(new ClipContext(
                rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return new Vec3(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        }
        return target;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("DirX")) entityData.set(DIR_X, tag.getFloat("DirX"));
        if (tag.contains("DirZ")) entityData.set(DIR_Z, tag.getFloat("DirZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putFloat("DirX", entityData.get(DIR_X));
        tag.putFloat("DirZ", entityData.get(DIR_Z));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
