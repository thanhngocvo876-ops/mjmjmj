package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * 纯质阳炎·烈阳火球实体：
 * 1. 投掷形态 (FLYING)：向准星高速飞去，蓄力越大尺寸与爆炸效果越大，命中造成 40 点直接伤害。
 * 2. 环绕护体形态 (ORBIT)：围绕玩家腰侧与斜上方公转，不挡视线，接触敌怪造成 15 点触碰伤害。
 */
public class PureFireBallEntity extends Entity {
    public static final int STATE_ORBIT = 0;
    public static final int STATE_FLYING = 1;
    public static final int ORBIT_RING_SIZE = 8;
    private static final double ORBIT_RADIUS = 2.35D;
    private static final double ORBIT_HEIGHT = 0.55D;
    private static final float ORBIT_SPEED = 0.045F;

    public static final float DIRECT_IMPACT_DAMAGE = 40.0F;
    public static final float CONTACT_DAMAGE = 15.0F;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
            PureFireBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHARGE_SCALE = SynchedEntityData.defineId(
            PureFireBallEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ORBIT_INDEX = SynchedEntityData.defineId(
            PureFireBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TOTAL_ORBITS = SynchedEntityData.defineId(
            PureFireBallEntity.class, EntityDataSerializers.INT);

    private int flightTicks = 0;
    private int contactCooldownTicks = 0;

    public PureFireBallEntity(EntityType<? extends PureFireBallEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** 创建投掷飞行火球 */
    public static PureFireBallEntity createFlyingBall(Level level, LivingEntity owner, Vec3 startPos, Vec3 direction, float chargeScale) {
        PureFireBallEntity ball = new PureFireBallEntity(ModEntities.PURE_FIRE_BALL.get(), level);
        ball.setOwner(owner);
        ball.setState(STATE_FLYING);
        ball.setChargeScale(chargeScale);
        ball.setPos(startPos.x, startPos.y, startPos.z);
        // 飞行速度 1.6 ~ 2.2
        float speed = 1.8F + chargeScale * 0.4F;
        ball.setDeltaMovement(direction.normalize().scale(speed));
        return ball;
    }

    /** 创建环绕火球 */
    public static PureFireBallEntity createOrbitBall(Level level, LivingEntity owner, int orbitIndex, int totalOrbits) {
        PureFireBallEntity ball = new PureFireBallEntity(ModEntities.PURE_FIRE_BALL.get(), level);
        ball.setOwner(owner);
        ball.setState(STATE_ORBIT);
        ball.setChargeScale(1.0F);
        ball.setOrbitIndex(orbitIndex);
        ball.setTotalOrbits(totalOrbits);
        ball.setPos(owner.getX(), owner.getY() + 1.0D, owner.getZ());
        return ball;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(STATE, STATE_FLYING);
        entityData.define(OWNER_ID, -1);
        entityData.define(CHARGE_SCALE, 1.0F);
        entityData.define(ORBIT_INDEX, 0);
        entityData.define(TOTAL_ORBITS, 1);
    }

    public int getState() {
        return entityData.get(STATE);
    }

    public void setState(int state) {
        entityData.set(STATE, state);
    }

    public void setOwner(LivingEntity owner) {
        entityData.set(OWNER_ID, owner != null ? owner.getId() : -1);
    }

    public LivingEntity getOwner() {
        Entity e = level().getEntity(entityData.get(OWNER_ID));
        return e instanceof LivingEntity living ? living : null;
    }

    public float getChargeScale() {
        return entityData.get(CHARGE_SCALE);
    }

    public void setChargeScale(float scale) {
        entityData.set(CHARGE_SCALE, Mth.clamp(scale, 0.5F, 2.5F));
    }

    public int getOrbitIndex() {
        return entityData.get(ORBIT_INDEX);
    }

    public void setOrbitIndex(int index) {
        entityData.set(ORBIT_INDEX, index);
    }

    public int getTotalOrbits() {
        return entityData.get(TOTAL_ORBITS);
    }

    public void setTotalOrbits(int total) {
        entityData.set(TOTAL_ORBITS, Math.max(1, total));
    }

    /** 让该环绕火球向目标方向激射 */
    public void launchToward(Vec3 direction, float chargeScale) {
        setState(STATE_FLYING);
        setChargeScale(chargeScale);
        float speed = 2.0F;
        setDeltaMovement(direction.normalize().scale(speed));
    }

    @Override
    public void tick() {
        super.tick();

        if (contactCooldownTicks > 0) {
            contactCooldownTicks--;
        }

        if (getState() == STATE_ORBIT) {
            tickOrbit();
        } else {
            tickFlying();
        }
    }

    private void tickOrbit() {
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }

        // 火球在玩家前、后、左、右及四个斜角均匀分布，整圈围绕玩家公转。
        int total = Mth.clamp(getTotalOrbits(), 1, ORBIT_RING_SIZE);
        int idx = Mth.clamp(getOrbitIndex(), 0, total - 1);
        double radius = total == ORBIT_RING_SIZE
                ? ORBIT_RADIUS
                : 1.85D + total * 0.06D;
        float baseAngle = (float) (2.0D * Math.PI * idx / total);
        float currentAngle = baseAngle + tickCount * ORBIT_SPEED;

        // 使用玩家朝向作为局部坐标，让“前后左右”始终相对玩家，而不是相对世界坐标。
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 offset = forward.scale(Math.sin(currentAngle) * radius)
                .add(right.scale(Math.cos(currentAngle) * radius));
        double targetX = owner.getX() + offset.x;
        double targetZ = owner.getZ() + offset.z;
        double bob = Math.sin((tickCount + idx * 7) * 0.06F) * 0.08D;
        double targetY = owner.getY() + owner.getBbHeight() * ORBIT_HEIGHT + bob;

        // 平滑插值跟随
        setPos(targetX, targetY, targetZ);

        if (!level().isClientSide) {
            // 环绕状态敌怪触碰检测 (每 2 ticks 检测一次，造成 15 点触碰伤害)
            if (tickCount % 2 == 0 && contactCooldownTicks <= 0) {
                AABB hitBox = getBoundingBox().inflate(0.55D);
                List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, hitBox,
                        e -> e != owner && e.isAlive() && !e.isAlliedTo(owner));
                if (!targets.isEmpty()) {
                    LivingEntity target = targets.get(0);
                    DamageSource source = owner instanceof ServerPlayer sp
                            ? level().damageSources().playerAttack(sp)
                            : level().damageSources().magic();
                    target.hurt(source, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, CONTACT_DAMAGE));
                    target.setSecondsOnFire(4);
                    level().playSound(null, getX(), getY(), getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    spawnExplosionParticles(0.8F);
                    discard(); // 触碰后该火球消耗
                }
            }
        } else {
            // 客户端粒子
            if (random.nextFloat() < 0.35F) {
                level().addParticle(ModParticles.PURE_FIRE.get(),
                        getX() + (random.nextDouble() - 0.5D) * 0.2D,
                        getY() + (random.nextDouble() - 0.5D) * 0.2D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.2D,
                        0.0D, 0.02D, 0.0D);
            }
        }
    }

    private void tickFlying() {
        flightTicks++;
        Vec3 movement = getDeltaMovement();
        Vec3 currentPos = position();
        Vec3 nextPos = currentPos.add(movement);

        // 射线命中检测
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }

        setPos(nextPos.x, nextPos.y, nextPos.z);

        if (level().isClientSide) {
            // 飞行轨迹尾焰粒子
            float scale = getChargeScale();
            int count = (int) (2 + scale * 2);
            for (int i = 0; i < count; i++) {
                double px = currentPos.x + (nextPos.x - currentPos.x) * (i / (double) count);
                double py = currentPos.y + (nextPos.y - currentPos.y) * (i / (double) count);
                double pz = currentPos.z + (nextPos.z - currentPos.z) * (i / (double) count);
                level().addParticle(ModParticles.PURE_FIRE.get(),
                        px + (random.nextDouble() - 0.5D) * 0.25D * scale,
                        py + (random.nextDouble() - 0.5D) * 0.25D * scale,
                        pz + (random.nextDouble() - 0.5D) * 0.25D * scale,
                        -movement.x * 0.15D, -movement.y * 0.15D, -movement.z * 0.15D);
            }
        }

        // 飞行超时 120 ticks (6秒) 自动消散
        if (!level().isClientSide && flightTicks > 120) {
            spawnExplosionParticles(getChargeScale());
            discard();
        }
    }

    protected boolean canHitEntity(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        LivingEntity owner = getOwner();
        if (entity == owner || (owner != null && entity.isAlliedTo(owner))) return false;
        return entity instanceof LivingEntity;
    }

    private void onHit(HitResult hit) {
        if (!level().isClientSide) {
            float scale = getChargeScale();
            LivingEntity owner = getOwner();
            DamageSource source = owner instanceof ServerPlayer sp
                    ? level().damageSources().playerAttack(sp)
                    : level().damageSources().magic();

            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
                // 直接命中爆发：40 点基础直接伤害 * 蓄力加成
                float damage = DIRECT_IMPACT_DAMAGE * (0.8F + scale * 0.3F);
                target.hurt(source, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, damage));
                target.setSecondsOnFire((int) (5 + scale * 3));
            }

            // 范围飞溅灼烧波
            double radius = 2.5D * scale;
            List<LivingEntity> splashTargets = level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(radius),
                    e -> e != owner && e.isAlive() && !e.isAlliedTo(owner));
            for (LivingEntity splash : splashTargets) {
                if (hit instanceof EntityHitResult ehr && splash == ehr.getEntity()) continue;
                splash.hurt(source, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, CONTACT_DAMAGE * scale));
                splash.setSecondsOnFire(4);
            }

            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.1F);
            spawnExplosionParticles(scale);
            discard();
        }
    }

    private void spawnExplosionParticles(float scale) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ModParticles.PURE_FIRE.get(),
                    getX(), getY(), getZ(),
                    (int) (30 * scale), 0.5D * scale, 0.5D * scale, 0.5D * scale, 0.15D);
            sl.sendParticles(ParticleTypes.FLAME,
                    getX(), getY(), getZ(),
                    (int) (20 * scale), 0.4D * scale, 0.4D * scale, 0.4D * scale, 0.12D);
            sl.sendParticles(ParticleTypes.LAVA,
                    getX(), getY(), getZ(),
                    (int) (8 * scale), 0.3D * scale, 0.3D * scale, 0.3D * scale, 0.05D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("State")) setState(tag.getInt("State"));
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("ChargeScale")) setChargeScale(tag.getFloat("ChargeScale"));
        if (tag.contains("OrbitIndex")) setOrbitIndex(tag.getInt("OrbitIndex"));
        if (tag.contains("TotalOrbits")) setTotalOrbits(tag.getInt("TotalOrbits"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("State", getState());
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putFloat("ChargeScale", getChargeScale());
        tag.putInt("OrbitIndex", getOrbitIndex());
        tag.putInt("TotalOrbits", getTotalOrbits());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
