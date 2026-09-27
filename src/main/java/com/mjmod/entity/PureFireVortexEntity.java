package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import com.mjmod.skill.PureFireSkillManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;

/** A real, server-synchronised 3D pure-fire vortex rather than particles. */
public class PureFireVortexEntity extends Projectile {
    private static final double MAX_DISTANCE = 15.0D;
    private static final int FADE_TICKS = 8;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.INT);
    private static final int STATE_CHARGING = 0;
    private static final int STATE_RELEASED = 1;
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FADE_START = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELEASE_START = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(
            PureFireVortexEntity.class, EntityDataSerializers.FLOAT);
    private Vec3 origin = Vec3.ZERO;

    public PureFireVortexEntity(EntityType<? extends PureFireVortexEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public PureFireVortexEntity(ServerLevel level, ServerPlayer owner, Vec3 origin, Vec3 direction) {
        this(ModEntities.PURE_FIRE_VORTEX.get(), level);
        setOwner(owner);
        this.origin = origin;
        Vec3 normalized = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0, 0, 1) : direction.normalize();
        entityData.set(DIR_X, (float) normalized.x);
        entityData.set(DIR_Y, (float) normalized.y);
        entityData.set(DIR_Z, (float) normalized.z);
        entityData.set(LENGTH, 1.8F);
        // Keep the controller at the palm origin.  The renderer extends the
        // centered model forward from this point, which avoids the old
        // midpoint-plus-visual-offset path that could stretch or jump when
        // skill 2 was active at the same time.
        setPos(origin);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(STATE, STATE_CHARGING);
        entityData.define(LENGTH, 1.8F);
        entityData.define(CHARGE, 0.0F);
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Y, 0.0F);
        entityData.define(DIR_Z, 1.0F);
        entityData.define(FADE_START, -1);
        entityData.define(RELEASE_START, -1);
        entityData.define(DAMAGE, 5.0F);
    }

    public Vec3 getFireDirection() {
        Vec3 dir = new Vec3(entityData.get(DIR_X), entityData.get(DIR_Y), entityData.get(DIR_Z));
        return dir.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : dir.normalize();
    }

    public float getLength() { return entityData.get(LENGTH); }
    public float getCharge() { return entityData.get(CHARGE); }
    public int getChargeStage() {
        float charge = getCharge();
        if (charge >= 1.0F) return 3;
        return Math.min(3, (int) (charge * 3.0F) + 1);
    }
    public float getDamage() { return entityData.get(DAMAGE); }
    public boolean isReleased() { return entityData.get(STATE) == STATE_RELEASED; }
    public boolean isFading() { return entityData.get(FADE_START) >= 0; }


    public float getAlpha(float partialTick) {
        int start = entityData.get(FADE_START);
        if (start < 0) return 1.0F;
        return Math.max(0.0F, 1.0F - (tickCount + partialTick - start) / FADE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) {
            beginFade();
        } else if (!isFading() && !isReleased() && PureFireSkillManager.isCasting(owner)) {
            // During the post-input action window the controller stays alive
            // at the palm but remains invisible.  The manager releases it only
            // after the player completes the hand motion.
            if (!PureFireSkillManager.isReleasing(owner)) {
                float charge = PureFireSkillManager.getChargeProgress(owner);
                entityData.set(CHARGE, charge);
                entityData.set(LENGTH, (float) (1.8D + (MAX_DISTANCE - 1.8D) * charge));
            }
            updateTransform(owner);
        } else if (!isFading() && isReleased()) {
            // Keep the short release animation attached to the caster while
            // movement is locked (the caster may still rotate their view).
            updateTransform(owner);
            if (tickCount % 2 == 0) spawnLaserColumnParticles((ServerLevel) level());
            int releaseStart = entityData.get(RELEASE_START);
            if (releaseStart >= 0) {
                if (tickCount - releaseStart < PureFireSkillManager.RELEASE_DURATION_TICKS
                        && tickCount % 4 == 0) {
                    applyDamage((ServerLevel) level(), owner);
                }
                if (tickCount - releaseStart >= PureFireSkillManager.RELEASE_DURATION_TICKS) {
                    beginFade();
                }
            }
        } else {
            beginFade();
        }
        if (isFading() && tickCount - entityData.get(FADE_START) >= FADE_TICKS) discard();
    }

    /** Recomputes the right-palm origin and aim from the owner's live crosshair. */
    private void updateTransform(ServerPlayer owner) {
        Vec3 direction = owner.getViewVector(1.0F);
        if (direction.lengthSqr() < 1.0E-6D) direction = getFireDirection();
        else direction = direction.normalize();
        entityData.set(DIR_X, (float) direction.x);
        entityData.set(DIR_Y, (float) direction.y);
        entityData.set(DIR_Z, (float) direction.z);
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) {
            double yaw = Math.toRadians(owner.getYRot());
            right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        } else {
            right = right.normalize();
        }
        // The animation's right shoulder is roughly 0.31 blocks from the
        // body centre and its straight arm reaches about 0.68 blocks. Calculating
        // the same palm anchor server-side keeps rendering and hit detection
        // attached to the animated hand for every observing client.
        Vec3 rightShoulder = owner.position()
                .add(0.0D, owner.getBbHeight() * 0.82D, 0.0D)
                .add(right.scale(0.31D));
        origin = rightShoulder.add(direction.scale(0.68D));
        // The entity remains at the hand/palm origin.  Collision still uses
        // the same origin. The player RenderLayer draws the visual from the
        // right-arm bone; direction, damage and particles use the same live aim.
        setPos(origin);
    }

    /** Cancels a weak/medium charge without applying damage. */
    public void cancelCharge() {
        if (isRemoved()) return;
        beginFade();
    }

    /** Starts the sustained attack.  The chosen charge stage is captured at
     * release time so later ticks cannot change its power. */
    public void release(int chargeStage) {
        if (isRemoved() || isReleased()) return;
        Entity ownerEntity = getOwner();
        if (!(ownerEntity instanceof ServerPlayer owner) || !(level() instanceof ServerLevel server)) {
            beginFade();
            return;
        }
        entityData.set(STATE, STATE_RELEASED);
        entityData.set(CHARGE, 1.0F);
        int stage = Math.max(1, Math.min(3, chargeStage));
        entityData.set(LENGTH, (float) PureFireSkillManager.getRangeForStage(stage));
        entityData.set(DAMAGE, stage == 1 ? 5.0F : stage == 2 ? 10.0F : 15.0F);
        updateTransform(owner);
        entityData.set(RELEASE_START, tickCount);
    }

    private void beginFade() {
        if (!isFading()) entityData.set(FADE_START, tickCount);
    }

    private void applyDamage(ServerLevel level, ServerPlayer owner) {
        Vec3 direction = getFireDirection();
        Vec3 start = origin;
        Vec3 end = start.add(direction.scale(getLength()));
        AABB area = new AABB(start, end).inflate(0.55D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != owner && e.isAlive() && !e.isSpectator())) {
            Optional<Vec3> hit = target.getBoundingBox().inflate(0.2D).clip(start, end);
            if (hit.isEmpty()) continue;
            target.invulnerableTime = 0;
            target.hurt(owner.damageSources().playerAttack(owner),
                    com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, getDamage()));
            target.invulnerableTime = 0;
        }
    }

    /** Sends a ring of custom embers around each segment of the active beam. */
    private void spawnLaserColumnParticles(ServerLevel level) {
        Vec3 direction = getFireDirection();
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(direction).normalize();
        int stage = getDamage() <= 5.0F ? 1 : getDamage() <= 10.0F ? 2 : 3;
        double radius = 0.16D + stage * 0.035D;
        for (double distance = 0.75D; distance <= getLength(); distance += 0.75D) {
            Vec3 center = origin.add(direction.scale(distance));
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2.0D * i / 6.0D + tickCount * 0.12D;
                Vec3 point = center.add(right.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                level.sendParticles(ModParticles.PURE_FIRE.get(), point.x, point.y, point.z,
                        1, 0.012D, 0.012D, 0.012D, 0.004D);
            }
            level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y, center.z,
                    1, 0.018D, 0.018D, 0.018D, 0.002D);
        }
    }

    @Override protected void onHit(net.minecraft.world.phys.HitResult result) {}
    @Override protected boolean canHitEntity(Entity entity) { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
