package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** 一道从虚空之泪右眼瞬间展开的斜上激光，保持片刻后渐隐。 */
public class VoidTearBeamEntity extends Projectile {
    private static final double MAX_DISTANCE = 35.0D;
    private static final float DEFAULT_DAMAGE = 10.0F;
    // Leave the entity alive long enough for a remote client to receive the
    // spawn packet, then fade it out instead of disappearing immediately.
    private static final int HOLD_TICKS = 8;
    private static final int FADE_TICKS = 16;
    private static final EntityDataAccessor<Integer> FADE_START_TICK =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM_LENGTH =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.FLOAT);
    // The spawn packet does not reliably preserve a projectile's motion for a
    // beam that is created and positioned at its midpoint in the same tick.
    // Keep the authored direction in synced data so the client renderer never
    // has to guess from a zero delta vector.
    private static final EntityDataAccessor<Float> DIRECTION_X =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BEAM_DAMAGE =
            SynchedEntityData.defineId(VoidTearBeamEntity.class, EntityDataSerializers.FLOAT);

    public VoidTearBeamEntity(EntityType<? extends VoidTearBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public VoidTearBeamEntity(Level level, LivingEntity shooter, Vec3 origin, Vec3 direction) {
        this(level, shooter, origin, direction, DEFAULT_DAMAGE);
    }

    /** Creates a beam with a skill-specific damage value while sharing the
     * exact first-skill model, fade timing and hit-effect pipeline. */
    public VoidTearBeamEntity(Level level, LivingEntity shooter, Vec3 origin,
                              Vec3 direction, float damage) {
        this(ModEntities.VOID_TEAR_PROJECTILE.get(), level);
        this.setOwner(shooter);
        this.setPos(origin);
        entityData.set(BEAM_DAMAGE, damage);
        Vec3 normalizedDirection = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        entityData.set(DIRECTION_X, (float) normalizedDirection.x);
        entityData.set(DIRECTION_Y, (float) normalizedDirection.y);
        entityData.set(DIRECTION_Z, (float) normalizedDirection.z);
        Vec3 end = origin.add(normalizedDirection.scale(MAX_DISTANCE));

        // Visuals and hit detection are deliberately independent. The model is
        // always a complete 35-block laser even when the crosshair points into
        // empty space; the ray-box test below only decides whether damage lands.
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            applyNearestEntityHit(serverLevel, shooter, origin, end);
        }
        entityData.set(BEAM_LENGTH, (float) MAX_DISTANCE);
        this.setPos(origin.add(normalizedDirection.scale(MAX_DISTANCE * 0.5D)));
        this.setDeltaMovement(normalizedDirection);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FADE_START_TICK, -1);
        entityData.define(BEAM_LENGTH, (float) MAX_DISTANCE);
        entityData.define(DIRECTION_X, 0.0F);
        entityData.define(DIRECTION_Y, 0.0F);
        entityData.define(DIRECTION_Z, 1.0F);
        entityData.define(BEAM_DAMAGE, DEFAULT_DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (!isFading() && tickCount >= HOLD_TICKS) {
            beginFade();
        }
        if (isFading() && tickCount - entityData.get(FADE_START_TICK) >= FADE_TICKS) {
            discard();
        }
    }

    private void beginFade() {
        if (!isFading()) {
            entityData.set(FADE_START_TICK, tickCount);
        }
    }

    public boolean isFading() {
        return entityData.get(FADE_START_TICK) >= 0;
    }

    public float getFadeAlpha(float partialTick) {
        int fadeStart = entityData.get(FADE_START_TICK);
        if (fadeStart < 0) {
            return 1.0F;
        }
        float progress = (tickCount + partialTick - fadeStart) / (float) FADE_TICKS;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - progress));
    }

    public float getBeamLength() {
        return entityData.get(BEAM_LENGTH);
    }

    /** Direction authored by the server when the three beams are cast. */
    public Vec3 getBeamDirection() {
        Vec3 direction = new Vec3(entityData.get(DIRECTION_X),
                entityData.get(DIRECTION_Y), entityData.get(DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    public float getAppearScale(float partialTick) {
        // Never render a zero-length first frame: on a remote client that
        // frame can be the only one seen before the next network update.
        return Math.max(0.30F, Math.min(1.0F, (tickCount + partialTick) / 2.0F));
    }

    private void applyNearestEntityHit(net.minecraft.server.level.ServerLevel serverLevel,
                                       LivingEntity shooter, Vec3 origin, Vec3 end) {
        AABB searchArea = new AABB(origin, end).inflate(1.0D);
        LivingEntity nearestTarget = null;
        Vec3 nearestHit = null;
        double nearestDistance = MAX_DISTANCE * MAX_DISTANCE;

        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != shooter && entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3> intersection = target.getBoundingBox().inflate(0.10D).clip(origin, end);
            if (intersection.isEmpty()) continue;
            double distance = origin.distanceToSqr(intersection.get());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = target;
                nearestHit = intersection.get();
            }
        }

        if (nearestTarget != null && nearestHit != null) {
            com.mjmod.skill.VoidTearsSkillManager.applyVoidTearHit(
                    serverLevel, shooter, nearestTarget,
                    new EntityHitResult(nearestTarget, nearestHit),
                    entityData.get(BEAM_DAMAGE));
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("BeamDamage", entityData.get(BEAM_DAMAGE));
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("BeamDamage")) {
            entityData.set(BEAM_DAMAGE, tag.getFloat("BeamDamage"));
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
