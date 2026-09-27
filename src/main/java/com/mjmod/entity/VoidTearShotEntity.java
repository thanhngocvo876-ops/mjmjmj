package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.skill.VoidTearsSkillManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;

/** 水盾释放的单发长泪光束。每道光束固定在发射根部，直达 35 格后渐隐。 */
public class VoidTearShotEntity extends Projectile {
    private static final double MAX_DISTANCE = 35.0D;
    private static final float DAMAGE = 10.0F;
    private static final int HOLD_TICKS = 18;
    private static final int FADE_TICKS = 12;
    private static final EntityDataAccessor<Integer> FADE_START_TICK =
            SynchedEntityData.defineId(VoidTearShotEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIRECTION_X =
            SynchedEntityData.defineId(VoidTearShotEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y =
            SynchedEntityData.defineId(VoidTearShotEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z =
            SynchedEntityData.defineId(VoidTearShotEntity.class, EntityDataSerializers.FLOAT);
    private Vec3 startPos;

    public VoidTearShotEntity(EntityType<? extends VoidTearShotEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public VoidTearShotEntity(Level level, LivingEntity owner, Vec3 origin, Vec3 direction) {
        this(ModEntities.VOID_TEAR_SHOT.get(), level);
        setOwner(owner);
        startPos = origin;
        Vec3 normalized = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
        entityData.set(DIRECTION_X, (float) normalized.x);
        entityData.set(DIRECTION_Y, (float) normalized.y);
        entityData.set(DIRECTION_Z, (float) normalized.z);
        // Position the entity at the midpoint, exactly like the first skill's
        // beam entity. The renderer therefore draws one root-to-tail rail,
        // rather than a short projectile travelling like an arrow.
        setPos(origin.add(normalized.scale(MAX_DISTANCE * 0.5D)));
        setDeltaMovement(Vec3.ZERO);
        if (level instanceof ServerLevel serverLevel) {
            applyNearestEntityHit(serverLevel, owner, origin,
                    origin.add(normalized.scale(MAX_DISTANCE)));
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FADE_START_TICK, -1);
        entityData.define(DIRECTION_X, 0.0F);
        entityData.define(DIRECTION_Y, 0.0F);
        entityData.define(DIRECTION_Z, 1.0F);
    }

    public Vec3 getShotDirection() {
        Vec3 direction = new Vec3(entityData.get(DIRECTION_X),
                entityData.get(DIRECTION_Y), entityData.get(DIRECTION_Z));
        return direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : direction.normalize();
    }

    public boolean isFading() {
        return entityData.get(FADE_START_TICK) >= 0;
    }

    public float getFadeAlpha(float partialTick) {
        int start = entityData.get(FADE_START_TICK);
        if (start < 0) return 1.0F;
        float progress = (tickCount + partialTick - start) / (float) FADE_TICKS;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - progress));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!isFading() && tickCount >= HOLD_TICKS) {
            entityData.set(FADE_START_TICK, tickCount);
        }
        if (isFading() && tickCount - entityData.get(FADE_START_TICK) >= FADE_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        discard();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // The burst is spawned as many separate laser streaks at nearly the
        // same instant. They must not collide with one another at the shield
        // surface and disappear before reaching a target.
        return entity != getOwner()
                && !(entity instanceof VoidTearShotEntity)
                && !(entity instanceof VoidTearShieldEntity)
                && super.canHitEntity(entity);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static void applyNearestEntityHit(ServerLevel level, LivingEntity owner,
                                              Vec3 origin, Vec3 end) {
        AABB searchArea = new AABB(origin, end).inflate(1.0D);
        LivingEntity nearestTarget = null;
        Vec3 nearestHit = null;
        double nearestDistance = MAX_DISTANCE * MAX_DISTANCE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
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
            targetHit(level, owner, nearestTarget, nearestHit);
        }
    }

    private static void targetHit(ServerLevel level, LivingEntity owner,
                                  LivingEntity target, Vec3 hitLocation) {
        target.invulnerableTime = 0;
        VoidTearsSkillManager.applyVoidTearHit(level, owner, target,
                new EntityHitResult(target, hitLocation), DAMAGE);
        target.invulnerableTime = 0;
    }
}
