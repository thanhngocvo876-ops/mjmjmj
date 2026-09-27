package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * 第五技能的自动护卫水珠。水珠从右眼沿弧线转移到玩家背后，待机时
 * 固定在随机编队位置；释放时沿玩家朝向直线飞行。
 */
public class VoidTearDropEntity extends Projectile {
    public static final int STATE_FORMING = 0;
    public static final int STATE_IDLE = 1;
    public static final int STATE_FLYING = 2;
    public static final int FORM_TICKS = 18;

    private static final double SPEED = 1.65D;
    public static final double MAX_DISTANCE = 35.0D;
    private static final float DAMAGE = 20.0F;
    private static final double HIT_RADIUS = 0.28D;

    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEHIND_OFFSET =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LATERAL_OFFSET =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT_OFFSET =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_X =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z =
            SynchedEntityData.defineId(VoidTearDropEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;
    private Vec3 formationOrigin;
    private double travelled;

    public VoidTearDropEntity(EntityType<? extends VoidTearDropEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public VoidTearDropEntity(ServerLevel level, ServerPlayer owner, Vec3 origin,
                              double behind, double lateral, double height) {
        this(ModEntities.VOID_TEAR_DROP.get(), level);
        setOwner(owner);
        ownerUuid = owner.getUUID();
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(BEHIND_OFFSET, (float) behind);
        entityData.set(LATERAL_OFFSET, (float) lateral);
        entityData.set(HEIGHT_OFFSET, (float) height);
        formationOrigin = origin;
        setPos(origin);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(STATE, STATE_FORMING);
        entityData.define(BEHIND_OFFSET, 1.0F);
        entityData.define(LATERAL_OFFSET, 0.0F);
        entityData.define(HEIGHT_OFFSET, 1.2F);
        entityData.define(DIRECTION_X, 0.0F);
        entityData.define(DIRECTION_Y, 0.0F);
        entityData.define(DIRECTION_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Keep the authored model readable: only one tiny vanilla water
            // mote every few ticks during transfer or flight.
            if (getState() != STATE_IDLE && tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.FALLING_WATER,
                        getX(), getY(), getZ(), 0.0D, -0.015D, 0.0D);
            }
            return;
        }

        ServerPlayer owner = getServerOwner();
        if (owner == null || !owner.isAlive()
                || owner.serverLevel() != level()
                || !VoidTearsSkillManager.hasPermission(owner)
                || !VoidTearsSkillManager.isActive(owner)) {
            discard();
            return;
        }

        if (getState() == STATE_FORMING) {
            tickForming(owner);
        } else if (getState() == STATE_IDLE) {
            setPos(getFormationPosition(owner));
            setDeltaMovement(Vec3.ZERO);
        } else {
            // Water beads are now only firing anchors. Legacy flying states
            // from older worlds are retired instead of performing the old
            // direct bead-attack behaviour.
            discard();
        }
    }

    private void tickForming(ServerPlayer owner) {
        if (formationOrigin == null) formationOrigin = position();
        Vec3 target = getFormationPosition(owner);
        float progress = Math.min(1.0F, tickCount / (float) FORM_TICKS);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        Vec3 curved = formationOrigin.scale(1.0D - eased).add(target.scale(eased))
                .add(0.0D, Math.sin(progress * Math.PI) * 0.48D, 0.0D);
        setPos(curved);
        setDeltaMovement(Vec3.ZERO);
        if (progress >= 1.0F) {
            entityData.set(STATE, STATE_IDLE);
            setPos(target);
        }
    }

    private void tickFlying(ServerPlayer owner) {
        Vec3 start = position();
        Vec3 direction = getFlightDirection();
        double step = SPEED;
        Vec3 end = start.add(direction.scale(step));
        spawnFlightTrail(start, end);
        BlockHitResult blockHit = level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE : start.distanceToSqr(blockHit.getLocation());

        Entity hitEntity = null;
        Vec3 hitLocation = null;
        double nearest = blockDistance;
        AABB path = new AABB(start, end).inflate(HIT_RADIUS);
        for (Entity candidate : level().getEntities(this, path,
                entity -> entity != owner && entity != this
                        && !(entity instanceof VoidTearDropEntity)
                        && !entity.isSpectator())) {
            AABB hitBox = candidate.getBoundingBox().inflate(HIT_RADIUS);
            Optional<Vec3> intersection = hitBox.contains(start)
                    ? Optional.of(start) : hitBox.clip(start, end);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceToSqr(intersection.get());
            if (distance < nearest) {
                nearest = distance;
                hitEntity = candidate;
                hitLocation = intersection.get();
            }
        }

        if (hitEntity != null && hitLocation != null) {
            hitEntity(owner, hitEntity, hitLocation);
            return;
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            discard();
            return;
        }

        // Move the projectile by teleporting its entity position. This avoids
        // vanilla projectile motion/owner interpolation dropping individual
        // beads while retaining a continuous swept collision check.
        teleportTo(end.x, end.y, end.z);
        setDeltaMovement(direction.scale(SPEED));
        hasImpulse = true;
        travelled += step;
        if (travelled >= MAX_DISTANCE || tickCount > FORM_TICKS + 160) discard();
    }

    private void hitEntity(ServerPlayer owner, Entity target, Vec3 hitLocation) {
        if (target instanceof LivingEntity living) {
            VoidTearsSkillManager.applyVoidTearHit((ServerLevel) level(), owner, living,
                    new EntityHitResult(living, hitLocation), DAMAGE);
        }
        discard();
    }

    private void spawnFlightTrail(Vec3 start, Vec3 end) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        Vec3 segment = end.subtract(start);
        for (int i = 0; i < 3; i++) {
            Vec3 point = start.add(segment.scale((i + 0.25D) / 3.0D));
            serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                    point.x, point.y, point.z, 1,
                    0.025D, 0.025D, 0.025D, 0.005D);
        }
        if (tickCount % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    start.x, start.y, start.z, 1,
                    0.08D, 0.08D, 0.08D, 0.012D);
        }
    }

    /** Starts a directional NBT projectile without requiring a living target. */
    @Deprecated
    public boolean launch(Vec3 direction) {
        // Kept only for binary/source compatibility with older worlds. The
        // fifth skill now consumes a bead and creates a VoidTearBeamEntity.
        return false;
    }

    private void beginFlight(Vec3 direction) {
        Vec3 normalized = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        entityData.set(STATE, STATE_FLYING);
        setFlightDirection(normalized);
        travelled = 0.0D;
        setDeltaMovement(normalized.scale(SPEED));
        // EntityType is configured with updateInterval(1); this flag also
        // requests the server tracker to send the current velocity immediately.
        hasImpulse = true;
    }

    private Vec3 getFormationPosition(ServerPlayer owner) {
        Vec3 forward = owner.getViewVector(1.0F);
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            float yaw = owner.getYRot() * ((float) Math.PI / 180.0F);
            forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            forward = forward.normalize();
        }
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        return owner.position().subtract(forward.scale(entityData.get(BEHIND_OFFSET)))
                .add(right.scale(entityData.get(LATERAL_OFFSET)))
                .add(0.0D, entityData.get(HEIGHT_OFFSET), 0.0D);
    }

    public int getState() {
        return entityData.get(STATE);
    }

    public boolean isIdle() {
        return getState() == STATE_IDLE;
    }

    /** Forming and idle beads can both be consumed; only flying beads are busy. */
    public boolean canLaunch() {
        return getState() != STATE_FLYING;
    }

    public boolean isForming() {
        return getState() == STATE_FORMING;
    }

    public float getFormationProgress(float partialTick) {
        if (!isForming()) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F,
                (tickCount + partialTick) / (float) FORM_TICKS));
    }

    public boolean isOwnedBy(ServerPlayer player) {
        return ownerUuid != null ? ownerUuid.equals(player.getUUID())
                : entityData.get(OWNER_ID) == player.getId();
    }

    private ServerPlayer getServerOwner() {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        if (ownerUuid != null) return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof ServerPlayer player ? player : null;
    }

    public Vec3 getFlightDirection() {
        Vec3 direction = new Vec3(entityData.get(DIRECTION_X),
                entityData.get(DIRECTION_Y), entityData.get(DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }

    private void setFlightDirection(Vec3 direction) {
        entityData.set(DIRECTION_X, (float) direction.x);
        entityData.set(DIRECTION_Y, (float) direction.y);
        entityData.set(DIRECTION_Z, (float) direction.z);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        entityData.set(STATE, tag.getInt("State"));
        entityData.set(BEHIND_OFFSET, tag.getFloat("Behind"));
        entityData.set(LATERAL_OFFSET, tag.getFloat("Lateral"));
        entityData.set(HEIGHT_OFFSET, tag.getFloat("Height"));
        if (tag.contains("DirectionX")) {
            entityData.set(DIRECTION_X, tag.getFloat("DirectionX"));
            entityData.set(DIRECTION_Y, tag.getFloat("DirectionY"));
            entityData.set(DIRECTION_Z, tag.getFloat("DirectionZ"));
        }
        travelled = tag.getDouble("Travelled");
        formationOrigin = position();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        tag.putInt("State", getState());
        tag.putFloat("Behind", entityData.get(BEHIND_OFFSET));
        tag.putFloat("Lateral", entityData.get(LATERAL_OFFSET));
        tag.putFloat("Height", entityData.get(HEIGHT_OFFSET));
        tag.putFloat("DirectionX", entityData.get(DIRECTION_X));
        tag.putFloat("DirectionY", entityData.get(DIRECTION_Y));
        tag.putFloat("DirectionZ", entityData.get(DIRECTION_Z));
        tag.putDouble("Travelled", travelled);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
