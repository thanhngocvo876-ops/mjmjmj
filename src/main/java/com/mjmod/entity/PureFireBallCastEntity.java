package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 纯质阳炎·火球施法控制器实体（绑定施法者）：
 * 状态机：
 * 1. CHARGING (蓄力中)：右手高举托起聚火，身体后引，火球持续放大。
 * 2. RELEASING (松手下砸/发射)：右手猛烈向前下方下砸抛掷，火球高速激射。
 */
public class PureFireBallCastEntity extends Entity {
    public static final int RELEASE_ACTION_TICKS = 8; // 下砸释放动作 0.4s (8 ticks)

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_SHIFT_MODE = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_RELEASED = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireBallCastEntity.class, EntityDataSerializers.FLOAT);

    private int releaseTickTimer = 0;
    private boolean ballFired = false;

    public PureFireBallCastEntity(EntityType<? extends PureFireBallCastEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireBallCastEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_BALL_CAST.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(IS_SHIFT_MODE, false);
        entityData.set(IS_RELEASED, false);
        entityData.set(CHARGE_TICKS, 0);

        Vec3 look = owner.getViewVector(1.0F).normalize();
        entityData.set(DIR_X, (float) look.x);
        entityData.set(DIR_Y, (float) look.y);
        entityData.set(DIR_Z, (float) look.z);

        setPos(owner.position());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(IS_SHIFT_MODE, false);
        entityData.define(IS_RELEASED, false);
        entityData.define(CHARGE_TICKS, 0);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Y, 0.0F);
        entityData.define(DIR_Z, 1.0F);
    }

    public LivingEntity getOwner() {
        Entity e = level().getEntity(entityData.get(OWNER_ID));
        return e instanceof LivingEntity living ? living : null;
    }

    public boolean isShiftMode() {
        return entityData.get(IS_SHIFT_MODE);
    }

    public boolean isReleased() {
        return entityData.get(IS_RELEASED);
    }

    public int getChargeTicks() {
        return entityData.get(CHARGE_TICKS);
    }

    public float getChargeScale() {
        // 0~60 ticks -> 1.0x ~ 2.2x 蓄力膨胀
        int ticks = getChargeTicks();
        return 1.0F + Mth.clamp(ticks / 50.0F, 0.0F, 1.2F);
    }

    public Vec3 getAimDirection() {
        Vec3 dir = new Vec3(entityData.get(DIR_X), entityData.get(DIR_Y), entityData.get(DIR_Z));
        return dir.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : dir.normalize();
    }

    /** 服务端玩家松开按键，进入下砸释放阶段 */
    public void releaseCast(Vec3 currentLook) {
        if (isReleased()) return;
        entityData.set(IS_RELEASED, true);
        Vec3 look = currentLook.normalize();
        entityData.set(DIR_X, (float) look.x);
        entityData.set(DIR_Y, (float) look.y);
        entityData.set(DIR_Z, (float) look.z);
    }

    public float getReleaseProgress(float partialTick) {
        if (!isReleased()) return 0.0F;
        return Mth.clamp((releaseTickTimer + partialTick) / (float) RELEASE_ACTION_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }

        setPos(owner.position());

        if (!isReleased()) {
            // 蓄力阶段
            int charge = entityData.get(CHARGE_TICKS) + 1;
            entityData.set(CHARGE_TICKS, charge);

            // 实时跟进施法者准星
            Vec3 look = owner.getViewVector(1.0F).normalize();
            entityData.set(DIR_X, (float) look.x);
            entityData.set(DIR_Y, (float) look.y);
            entityData.set(DIR_Z, (float) look.z);

            if (!level().isClientSide) {
                // 蓄力期间由缓慢效果实时体现火球的庞大与沉重感
                if (owner instanceof ServerPlayer sp) {
                    // chargeTicks 越大，火球越大，缓慢等级越高 (最高 缓慢 IV)
                    int slownessLevel = Math.min(3, charge / 16);
                    sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 6, slownessLevel, false, false, true));
                }
            } else {
                // 客户端单手手掌上方聚火粒子预览
                if (random.nextFloat() < 0.65F) {
                    Vec3 handPos = getRightHandWorldPos(owner);
                    level().addParticle(ModParticles.PURE_FIRE.get(),
                            handPos.x + (random.nextDouble() - 0.5D) * 0.4D,
                            handPos.y + (random.nextDouble() - 0.5D) * 0.4D,
                            handPos.z + (random.nextDouble() - 0.5D) * 0.4D,
                            0.0D, 0.04D, 0.0D);
                }
            }
        } else {
            // 松手后猛烈下砸动作阶段
            releaseTickTimer++;

            // 在动作第 3 tick (手臂完全下劈前推瞬间) 脱手生成飞行火球实体
            if (!level().isClientSide && !ballFired && releaseTickTimer >= 3) {
                ballFired = true;
                ServerLevel sl = (ServerLevel) level();
                Vec3 aim = getAimDirection();
                Vec3 spawnPos = owner.getEyePosition(1.0F).add(aim.scale(1.2D));
                float scale = getChargeScale();
                PureFireBallEntity flyingBall = PureFireBallEntity.createFlyingBall(
                        sl, owner, spawnPos, aim, scale);
                sl.addFreshEntity(flyingBall);
                sl.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.3F, 0.9F);
            }

            if (!level().isClientSide && releaseTickTimer >= RELEASE_ACTION_TICKS) {
                discard();
            }
        }
    }

    public static Vec3 getRightHandWorldPos(LivingEntity entity) {
        Vec3 look = entity.getViewVector(1.0F).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        return entity.getEyePosition(1.0F).add(right.scale(0.35D)).add(look.scale(0.4D)).add(0, 0.25D, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("ShiftMode")) entityData.set(IS_SHIFT_MODE, tag.getBoolean("ShiftMode"));
        if (tag.contains("Released")) entityData.set(IS_RELEASED, tag.getBoolean("Released"));
        if (tag.contains("ChargeTicks")) entityData.set(CHARGE_TICKS, tag.getInt("ChargeTicks"));
        if (tag.contains("DirX")) entityData.set(DIR_X, tag.getFloat("DirX"));
        if (tag.contains("DirY")) entityData.set(DIR_Y, tag.getFloat("DirY"));
        if (tag.contains("DirZ")) entityData.set(DIR_Z, tag.getFloat("DirZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putBoolean("ShiftMode", entityData.get(IS_SHIFT_MODE));
        tag.putBoolean("Released", entityData.get(IS_RELEASED));
        tag.putInt("ChargeTicks", entityData.get(CHARGE_TICKS));
        tag.putFloat("DirX", entityData.get(DIR_X));
        tag.putFloat("DirY", entityData.get(DIR_Y));
        tag.putFloat("DirZ", entityData.get(DIR_Z));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
