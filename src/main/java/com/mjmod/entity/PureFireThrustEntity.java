package com.mjmod.entity;

import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import com.mjmod.skill.PureFireSkillManager;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Comparator;
import java.util.List;

/**
 * 七技能【穿刺】（Thrust / 瞬影贯心）实体控制器：
 * 时长 27 tick（1.125 秒），对齐“刺杀as老冯”动画。
 * 1. 3格锁敌：若有目标，锁定胸口贯穿；空刺突进5格（带直线破空气浪）。
 * 2. 伤害判定：普通 30 点；背后判定（夹角 <= 60°）暗杀 45 点且暴击火花。
 * 3. 目标获得专属 Impalement 状态 + 缓慢 V + 虚弱 I，无法移动还击。
 * 4. 目标胸部附着心脏空洞（Chest Void）Overlay / 粒子遮罩，拔出时金红火柱结算伤害与解除。
 */
public final class PureFireThrustEntity extends Entity {
    public static final int DURATION_TICKS = 27;
    public static final int STRIKE_TICK = 9;   // ~0.375s
    public static final int HOLD_TICK = 17;    // ~0.708s
    public static final float NORMAL_DAMAGE = 30.0F;
    public static final float BACKSTAB_DAMAGE = 45.0F;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_BACKSTAB = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MISSED_DASH = SynchedEntityData.defineId(
            PureFireThrustEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean initialExecuted = false;
    private boolean finalDamageSettled = false;

    public PureFireThrustEntity(EntityType<? extends PureFireThrustEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireThrustEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_THRUST.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        Vec3 look = owner.getViewVector(1.0F).normalize();
        entityData.set(DIR_X, (float) look.x);
        entityData.set(DIR_Y, (float) look.y);
        entityData.set(DIR_Z, (float) look.z);
        setPos(owner.position());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(TARGET_ID, -1);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Y, 0.0F);
        entityData.define(DIR_Z, 1.0F);
        entityData.define(IS_BACKSTAB, false);
        entityData.define(MISSED_DASH, false);
    }

    public LivingEntity getOwner() {
        Entity e = level().getEntity(entityData.get(OWNER_ID));
        return e instanceof LivingEntity living ? living : null;
    }

    public LivingEntity getTarget() {
        int id = entityData.get(TARGET_ID);
        if (id == -1) return null;
        Entity e = level().getEntity(id);
        return e instanceof LivingEntity living ? living : null;
    }

    public boolean isBackstab() {
        return entityData.get(IS_BACKSTAB);
    }

    public boolean isMissedDash() {
        return entityData.get(MISSED_DASH);
    }

    public Vec3 getThrustDirection() {
        Vec3 dir = new Vec3(entityData.get(DIR_X), entityData.get(DIR_Y), entityData.get(DIR_Z));
        return dir.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : dir.normalize();
    }

    public float getAnimationProgress(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / (float) DURATION_TICKS, 0.0F, 1.0F);
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

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            if (!initialExecuted) {
                initialExecuted = true;
                executeDetectionAndThrust(serverLevel, owner);
            }

            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                // Keep target pinned and apply debuff while impaled
                if (tickCount < DURATION_TICKS) {
                    target.addEffect(new MobEffectInstance(ModEffects.IMPALEMENT.get(), 10, 0, false, false, true));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false, false));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 0, false, false, false));
                    target.setDeltaMovement(0, 0, 0);

                    // Hollow Heart visual particles at chest level
                    spawnHollowHeartParticles(serverLevel, target);
                }

                // Final extraction burst & settlement
                if (tickCount >= DURATION_TICKS && !finalDamageSettled) {
                    finalDamageSettled = true;
                    settleFinalStrike(serverLevel, owner, target);
                }
            }

            if (tickCount >= DURATION_TICKS) {
                if (owner instanceof ServerPlayer player) {
                    PureFireSkillManager.holdFireBlade(player);
                }
                discard();
            }
        }
    }

    private void executeDetectionAndThrust(ServerLevel level, LivingEntity owner) {
        Vec3 look = getThrustDirection();
        Vec3 eyePos = owner.getEyePosition();

        // 3 blocks detection sphere/cone
        AABB searchBox = owner.getBoundingBox().expandTowards(look.scale(3.0D)).inflate(1.2D);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != owner && e.isAlive() && !e.isAlliedTo(owner));

        LivingEntity bestTarget = potentialTargets.stream()
                .filter(e -> {
                    Vec3 toTarget = e.position().add(0, e.getBbHeight() * 0.5D, 0).subtract(eyePos);
                    double dist = toTarget.length();
                    if (dist > 3.8D) return false;
                    double dot = toTarget.normalize().dot(look);
                    return dot > 0.45D; // cone facing
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(owner)))
                .orElse(null);

        if (bestTarget != null) {
            // Target locked!
            entityData.set(TARGET_ID, bestTarget.getId());

            // Check backstab angle:
            // Angle between owner's facing and target's facing <= 60 deg (cos >= 0.5)
            Vec3 ownerFacing = new Vec3(look.x, 0, look.z).normalize();
            Vec3 targetFacing = bestTarget.getViewVector(1.0F);
            targetFacing = new Vec3(targetFacing.x, 0, targetFacing.z).normalize();
            double angleDot = ownerFacing.dot(targetFacing);
            boolean backstab = angleDot >= 0.5D; // <= 60 deg
            entityData.set(IS_BACKSTAB, backstab);

            // Pull owner close to target chest
            Vec3 targetChest = bestTarget.position().add(0, bestTarget.getBbHeight() * 0.5D, 0);
            Vec3 snapPos = targetChest.subtract(look.scale(0.85D));
            owner.teleportTo(snapPos.x, bestTarget.getY(), snapPos.z);
            owner.setDeltaMovement(0, 0, 0);

            // Audio & impact
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.3F);
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.8F);

            if (backstab) {
                level.playSound(null, bestTarget.getX(), bestTarget.getY(), bestTarget.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5F, 0.8F);
                level.sendParticles(ParticleTypes.CRIT, bestTarget.getX(), bestTarget.getY() + 1.0D, bestTarget.getZ(),
                        25, 0.4D, 0.4D, 0.4D, 0.3D);
            }
        } else {
            // Empty thrust dash 5 blocks forward
            entityData.set(MISSED_DASH, true);
            Vec3 targetPos = owner.position().add(look.scale(5.0D));

            // Sound
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.6F);
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9F, 1.4F);

            // Straight line streak particles
            for (double d = 0.5D; d <= 5.0D; d += 0.4D) {
                Vec3 p = owner.getEyePosition().add(look.scale(d));
                level.sendParticles(ModParticles.PURE_FIRE.get(), p.x, p.y - 0.2D, p.z,
                        2, 0.05D, 0.05D, 0.05D, 0.01D);
            }

            owner.setDeltaMovement(look.scale(1.2D));
            owner.hurtMarked = true;
        }
    }

    private void spawnHollowHeartParticles(ServerLevel level, LivingEntity target) {
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.62D, 0);
        // Pure fire flames swirling around hollow core
        for (int i = 0; i < 4; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double rad = 0.28D + random.nextDouble() * 0.15D;
            double px = chest.x + Math.cos(angle) * rad;
            double pz = chest.z + Math.sin(angle) * rad;
            double py = chest.y + (random.nextDouble() - 0.5D) * 0.3D;
            level.sendParticles(ModParticles.PURE_FIRE.get(), px, py, pz, 1, 0.01D, 0.02D, 0.01D, 0.01D);
        }
        // Black void smoke inside center
        level.sendParticles(ParticleTypes.SMOKE, chest.x, chest.y, chest.z, 2, 0.08D, 0.08D, 0.08D, 0.005D);
    }

    private void settleFinalStrike(ServerLevel level, LivingEntity owner, LivingEntity target) {
        float damage = isBackstab() ? BACKSTAB_DAMAGE : NORMAL_DAMAGE;
        DamageSource ds = owner instanceof ServerPlayer sp
                ? level.damageSources().playerAttack(sp)
                : level.damageSources().mobAttack(owner);

        target.removeEffect(ModEffects.IMPALEMENT.get());
        target.hurt(ds, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, damage));
        target.setSecondsOnFire(5);

        // Final golden-red flame pillar eruption
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6D, 0);
        for (double yOff = -0.5D; yOff <= 2.5D; yOff += 0.3D) {
            level.sendParticles(ModParticles.PURE_FIRE.get(),
                    chest.x, chest.y + yOff, chest.z,
                    8, 0.2D, 0.1D, 0.2D, 0.05D);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 1.5F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("TargetId")) entityData.set(TARGET_ID, tag.getInt("TargetId"));
        if (tag.contains("DirX")) entityData.set(DIR_X, tag.getFloat("DirX"));
        if (tag.contains("DirY")) entityData.set(DIR_Y, tag.getFloat("DirY"));
        if (tag.contains("DirZ")) entityData.set(DIR_Z, tag.getFloat("DirZ"));
        if (tag.contains("IsBackstab")) entityData.set(IS_BACKSTAB, tag.getBoolean("IsBackstab"));
        if (tag.contains("MissedDash")) entityData.set(MISSED_DASH, tag.getBoolean("MissedDash"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("TargetId", entityData.get(TARGET_ID));
        tag.putFloat("DirX", entityData.get(DIR_X));
        tag.putFloat("DirY", entityData.get(DIR_Y));
        tag.putFloat("DirZ", entityData.get(DIR_Z));
        tag.putBoolean("IsBackstab", entityData.get(IS_BACKSTAB));
        tag.putBoolean("MissedDash", entityData.get(MISSED_DASH));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
