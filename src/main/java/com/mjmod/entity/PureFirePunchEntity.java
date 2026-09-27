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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Three-phase controller for the pure-fire straight punch. The player's
 * Blockbench animation is rendered on the client; this entity owns the
 * authoritative target scan, dash, debuffs, impact and internal explosion.
 */
public final class PureFirePunchEntity extends Entity {
    public static final int CHARGE_TICKS = 11;
    public static final int DASH_TICKS = 20;
    public static final int RETRACT_TICKS = 6;
    public static final int DURATION_TICKS = CHARGE_TICKS + DASH_TICKS + RETRACT_TICKS;
    private static final int DASH_START_TICK = CHARGE_TICKS + 1;
    private static final int RETRACT_START_TICK = DASH_START_TICK + DASH_TICKS;
    private static final double REACH = 16.0D;
    private static final double HALF_WIDTH = 3.0D;
    private static final double BEHIND_DISTANCE = 6.0D;
    private static final int FREE_DASH_TICKS = 6;
    private static final double FREE_DASH_DISTANCE = REACH;
    private static final float IMPACT_DAMAGE = 18.0F;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFirePunchEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFirePunchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFirePunchEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFirePunchEntity.class, EntityDataSerializers.FLOAT);
    /** Server-side target lock captured during the preparation/push window. */
    private final Set<Integer> lockedTargetIds = new HashSet<>();
    private final Map<Integer, SavedEffects> savedEffects = new HashMap<>();
    private int dashTargetId = -1;
    private boolean dashCompleted;
    private Vec3 lockedAimDirection = Vec3.ZERO;
    private Vec3 freeDashStart;
    private MobEffectInstance savedResistance;
    private boolean chargeSoundPlayed;
    private boolean dashSoundPlayed;
    private boolean retractSoundPlayed;
    private boolean explosionSoundPlayed;
    private boolean resistanceSaved;

    public PureFirePunchEntity(EntityType<? extends PureFirePunchEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFirePunchEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_PUNCH.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        // Preserve the full activation-time crosshair direction for the
        // no-target dash. The target lane below remains horizontal so its
        // existing lock-on behaviour and width are unchanged.
        Vec3 look = owner.getViewVector(1.0F);
        lockedAimDirection = look.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : look.normalize();
        entityData.set(DIR_X, (float) lockedAimDirection.x);
        entityData.set(DIR_Y, (float) lockedAimDirection.y);
        entityData.set(DIR_Z, (float) lockedAimDirection.z);
        setPos(owner.position());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Y, 0.0F);
        entityData.define(DIR_Z, 1.0F);
    }

    public LivingEntity getOwner() {
        Entity owner = level().getEntity(entityData.get(OWNER_ID));
        return owner instanceof LivingEntity living ? living : null;
    }

    public Vec3 getPunchDirection() {
        Vec3 direction = new Vec3(entityData.get(DIR_X), entityData.get(DIR_Y),
                entityData.get(DIR_Z));
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    public float getAnimationProgress(float partialTick) {
        return getModelAnimationProgress(partialTick);
    }

    /** Progress of the uploaded 0.54167-second Blockbench clip. */
    public float getModelAnimationProgress(float partialTick) {
        float time = tickCount + partialTick;
        if (time <= CHARGE_TICKS) {
            return Math.max(0.0F, Math.min(1.0F, time / CHARGE_TICKS));
        }
        if (time < RETRACT_START_TICK) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F,
                1.0F - (time - RETRACT_START_TICK) / RETRACT_TICKS));
    }

    public int getPhase() {
        if (tickCount <= CHARGE_TICKS) return 0;
        if (tickCount < RETRACT_START_TICK) return 1;
        if (tickCount <= DURATION_TICKS) return 2;
        return 3;
    }

    public float getPhaseProgress(float partialTick) {
        float time = tickCount + partialTick;
        if (time <= CHARGE_TICKS) return Math.max(0.0F, time / CHARGE_TICKS);
        if (time < RETRACT_START_TICK) {
            return Math.max(0.0F, Math.min(1.0F,
                    (time - DASH_START_TICK) / DASH_TICKS));
        }
        return Math.max(0.0F, Math.min(1.0F,
                (time - RETRACT_START_TICK) / RETRACT_TICKS));
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) {
                restoreEffects((ServerLevel) level());
                if (owner != null) restoreResistance(owner);
                discard();
            }
            return;
        }
        if (level().isClientSide) return;

        ServerLevel server = (ServerLevel) level();
        if (tickCount <= CHARGE_TICKS) {
            lockTargets(server, owner);
            applyChargeSlowdown(server);
            applyChargeResistance(owner);
            if (!chargeSoundPlayed) {
                owner.playSound(SoundEvents.BEACON_POWER_SELECT, 0.85F, 0.72F);
                chargeSoundPlayed = true;
            }
        }
        if (tickCount >= DASH_START_TICK && tickCount < RETRACT_START_TICK) {
            dashBehindTarget(server, owner);
            spawnDashFlame(server, owner);
            if (!dashSoundPlayed) {
                owner.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.15F, 0.72F);
                dashSoundPlayed = true;
            }
        }
        if (tickCount >= RETRACT_START_TICK && tickCount <= DURATION_TICKS) {
            if (tickCount == RETRACT_START_TICK) {
                // The dash has arrived behind the primary target. The
                // stronger hit debuffs begin exactly as the retract starts.
                applyImpactDebuffs(server);
                owner.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.82F);
                retractSoundPlayed = true;
            }
        }
        if (tickCount >= DURATION_TICKS) {
            // The 0.3-second retract has completed: explode inside every
            // locked target, then restore any pre-existing effects.
            explodeInsideTargets(server, owner);
            if (!explosionSoundPlayed) {
                owner.playSound(SoundEvents.GENERIC_EXPLODE, 0.8F, 1.15F);
                explosionSoundPlayed = true;
            }
            restoreEffects(server);
            restoreResistance(owner);
            discard();
        }
    }

    private void applyChargeSlowdown(ServerLevel level) {
        for (Integer id : lockedTargetIds) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                saveEffects(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        4, 2, false, true, true));
            }
        }
    }

    private void applyChargeResistance(LivingEntity owner) {
        if (!resistanceSaved) {
            savedResistance = owner.getEffect(MobEffects.DAMAGE_RESISTANCE);
            resistanceSaved = true;
        }
        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4, 1,
                false, false, true));
    }

    private void restoreResistance(LivingEntity owner) {
        owner.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (savedResistance != null) owner.addEffect(savedResistance);
        savedResistance = null;
        resistanceSaved = false;
    }

    private void applyImpactDebuffs(ServerLevel level) {
        for (Integer id : lockedTargetIds) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity target && target.isAlive()) {
                saveEffects(target);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        DASH_TICKS + RETRACT_TICKS + 4, 4, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        DASH_TICKS + RETRACT_TICKS + 4, 4, false, true, true));
            }
        }
    }

    private void explodeInsideTargets(ServerLevel level, LivingEntity owner) {
        for (Integer id : lockedTargetIds) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity target)
                    || target == owner || !target.isAlive() || target.isSpectator()) continue;
            target.hurt(owner.damageSources().mobAttack(owner),
                    com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, IMPACT_DAMAGE));
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
            level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y, center.z,
                    24, 0.42D, 0.55D, 0.42D, 0.035D);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.8F, 1.25F);
        }
    }

    private void spawnDashFlame(ServerLevel level, LivingEntity owner) {
        Vec3 center = owner.position().add(0.0D, owner.getBbHeight() * 0.58D, 0.0D);
        Vec3 direction = getPunchDirection();
        // Large, obvious streak around the dash body; this is skill 3's
        // movement effect and is independent of the skill-2 glove model.
        level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y, center.z,
                18, 0.38D, 0.62D, 0.38D, 0.045D);
        Vec3 tail = center.subtract(direction.scale(0.55D));
        level.sendParticles(ModParticles.PURE_FIRE.get(), tail.x, tail.y, tail.z,
                10, 0.24D, 0.42D, 0.24D, 0.03D);
    }

    private void dashBehindTarget(ServerLevel level, LivingEntity owner) {
        if (dashCompleted) return;
        LivingEntity target = getDashTarget(level);
        if (target == null) {
            dashTowardCrosshair(owner);
            return;
        }
        Vec3 behind = getBehindPosition(target);
        // Cross the target in one server tick and appear six blocks behind it.
        owner.teleportTo(behind.x, behind.y, behind.z);
        setPos(behind);
        dashCompleted = true;
    }

    private void dashTowardCrosshair(LivingEntity owner) {
        if (freeDashStart == null) freeDashStart = owner.position();
        Vec3 direction = lockedAimDirection.lengthSqr() < 1.0E-6D
                ? getPunchDirection() : lockedAimDirection;
        Vec3 destination = freeDashStart.add(direction.scale(FREE_DASH_DISTANCE));

        int elapsed = Math.max(1, tickCount - DASH_START_TICK + 1);
        double progress = Math.min(1.0D, elapsed / (double) FREE_DASH_TICKS);
        Vec3 next = freeDashStart.lerp(destination, progress);
        owner.teleportTo(next.x, next.y, next.z);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0.0F;
        owner.hurtMarked = true;
        setPos(next);
        if (elapsed >= FREE_DASH_TICKS) dashCompleted = true;
    }

    private LivingEntity getDashTarget(ServerLevel level) {
        if (dashTargetId >= 0) {
            Entity current = level.getEntity(dashTargetId);
            if (current instanceof LivingEntity living && living.isAlive()) return living;
        }
        LivingEntity best = null;
        double distance = Double.MAX_VALUE;
        for (Integer id : lockedTargetIds) {
            Entity entity = level.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                double candidate = distanceToSqrEntity(living);
                if (candidate < distance) {
                    distance = candidate;
                    best = living;
                    dashTargetId = living.getId();
                }
            }
        }
        return best;
    }

    private Vec3 getBehindPosition(LivingEntity target) {
        Vec3 facing = target.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-5D) {
            float yaw = target.getYRot() * ((float) Math.PI / 180.0F);
            horizontal = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            horizontal = horizontal.normalize();
        }
        return target.position().subtract(horizontal.scale(BEHIND_DISTANCE))
                .add(0.0D, 0.04D, 0.0D);
    }

    private double distanceToSqrEntity(Entity entity) {
        return distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
    }

    private void saveEffects(LivingEntity target) {
        savedEffects.computeIfAbsent(target.getId(), ignored -> new SavedEffects(
                target.getEffect(MobEffects.MOVEMENT_SLOWDOWN),
                target.getEffect(MobEffects.WEAKNESS)));
    }

    private void restoreEffects(ServerLevel level) {
        for (Map.Entry<Integer, SavedEffects> entry : savedEffects.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target)) continue;
            target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            target.removeEffect(MobEffects.WEAKNESS);
            SavedEffects saved = entry.getValue();
            if (saved.slowdown != null) target.addEffect(saved.slowdown);
            if (saved.weakness != null) target.addEffect(saved.weakness);
        }
        savedEffects.clear();
    }

    private record SavedEffects(MobEffectInstance slowdown,
                                MobEffectInstance weakness) {}


    /** Locks entities in an oriented six-block by four-block punch lane. */
    private void lockTargets(ServerLevel level, LivingEntity owner) {
        Vec3 aim = getPunchDirection();
        Vec3 direction = new Vec3(aim.x, 0.0D, aim.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            float yaw = owner.getYRot() * ((float) Math.PI / 180.0F);
            direction = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            direction = direction.normalize();
        }
        Vec3 start = getFistAnchor(owner);
        AABB search = owner.getBoundingBox().inflate(REACH + HALF_WIDTH, 2.0D,
                REACH + HALF_WIDTH);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
            Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 delta = center.subtract(start);
            double along = delta.dot(direction);
            if (along < 0.0D || along > REACH) continue;
            double lateral = delta.subtract(direction.scale(along)).length();
            if (lateral <= HALF_WIDTH + target.getBbWidth() * 0.5D) {
                lockedTargetIds.add(target.getId());
            }
        }
    }

    private Vec3 getFistAnchor(LivingEntity owner) {
        Vec3 direction = getPunchDirection();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(-Math.cos(Math.toRadians(owner.getYRot())),
                    0.0D, -Math.sin(Math.toRadians(owner.getYRot())));
        } else {
            right = right.normalize();
        }
        return owner.position().add(0.0D, owner.getBbHeight() * 0.72D, 0.0D)
                .add(right.scale(0.34D)).add(direction.scale(0.22D));
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
