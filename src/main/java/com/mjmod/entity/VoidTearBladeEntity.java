package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 泪刃：从施法者右眼位置成形后，沿准心方向进行一次竖向斩击。
 * 实体本身负责 60 格路径和 6 格高的命中判定，客户端 renderer 负责
 * Blockbench 风格的成形、挥斩和渐隐动画。
 */
public class VoidTearBladeEntity extends Projectile {
    public static final double MAX_DISTANCE = 60.0D;
    public static final double BLADE_HEIGHT = 6.0D;
    /** 凝聚蓄力时长；泪刃在这段时间内停在右眼位置。 */
    public static final int CHARGE_TICKS = 20;
    /** 泪刃从一格高成长到四格高所用的飞行时间。 */
    public static final int GROW_TICKS = 20;
    public static final int FADE_TICKS = 12;
    private static final double SPEED = 3.0D;
    private static final float DAMAGE = 50.0F;

    private static final EntityDataAccessor<Integer> FADE_START_TICK =
            SynchedEntityData.defineId(VoidTearBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIRECTION_X =
            SynchedEntityData.defineId(VoidTearBladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y =
            SynchedEntityData.defineId(VoidTearBladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z =
            SynchedEntityData.defineId(VoidTearBladeEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private double travelled;
    private final Set<UUID> hitTargets = new HashSet<>();

    public VoidTearBladeEntity(EntityType<? extends VoidTearBladeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public VoidTearBladeEntity(Level level, LivingEntity owner, Vec3 origin, Vec3 direction) {
        this(ModEntities.VOID_TEAR_BLADE.get(), level);
        setOwner(owner);
        setPos(origin);
        this.direction = direction.lengthSqr() < 1.0E-6D
                ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
        entityData.set(DIRECTION_X, (float) this.direction.x);
        entityData.set(DIRECTION_Y, (float) this.direction.y);
        entityData.set(DIRECTION_Z, (float) this.direction.z);
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FADE_START_TICK, -1);
        entityData.define(DIRECTION_X, 0.0F);
        entityData.define(DIRECTION_Y, 0.0F);
        entityData.define(DIRECTION_Z, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        if (tickCount <= CHARGE_TICKS && owner != null) {
            // 蓄力期间持续刷新短时缓慢 III，让玩家明显感到正在凝聚剑气。
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    4, 2, false, false, false));
            // A restrained amount of vanilla water makes the crescent gather
            // visibly without turning the charge into a particle cloud.
            if (tickCount % 3 == 0) {
                spawnWaterParticles(position(), false);
            }
        }

        // The blade remains at the eye anchor while its sword-aura silhouette
        // gathers. At the end of this phase it starts its straight flight.
        if (tickCount < CHARGE_TICKS) {
            if (owner != null) setPos(getOwnerAnchor(owner));
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (travelled < MAX_DISTANCE) {
            Vec3 start = position();
            double step = Math.min(SPEED, MAX_DISTANCE - travelled);
            Vec3 end = start.add(getBladeDirection().scale(step));
            applyHits((ServerLevel) level(), start, end);
            setPos(end);
            travelled += step;
            // Leave a short, sparse water trail behind the moving blade. The
            // authored model remains the main effect; these particles only
            // sell its wet, flowing edge.
            if (tickCount % 2 == 0) {
                spawnWaterParticles(end.subtract(getBladeDirection().scale(0.55D)), true);
            }
            return;
        }

        if (!isFading()) {
            entityData.set(FADE_START_TICK, tickCount);
        } else if (tickCount - entityData.get(FADE_START_TICK) >= FADE_TICKS) {
            discard();
        }
    }

    private void applyHits(ServerLevel level, Vec3 start, Vec3 end) {
        AABB path = new AABB(start, end).inflate(0.95D, BLADE_HEIGHT * 0.5D, 0.95D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path,
                entity -> entity != getOwner() && entity.isAlive() && !entity.isSpectator()
                        && !hitTargets.contains(entity.getUUID()))) {
            Optional<Vec3> intersection = target.getBoundingBox().inflate(0.12D).clip(start, end);
            Vec3 hitLocation = intersection.orElseGet(() ->
                    target.getBoundingBox().getCenter());
            hitTargets.add(target.getUUID());
            if (!(getOwner() instanceof LivingEntity owner)) continue;
            VoidTearsSkillManager.applyVoidTearHit(level, owner, target,
                    new EntityHitResult(target, hitLocation), DAMAGE);
            // Severing is exclusive to the blade. Shared Void Tears hit effects
            // remain centralized without changing skills 1, 2 or future beams.
            target.addEffect(new MobEffectInstance(ModEffects.SEVERING.get(),
                    200, 0, false, true, true));
        }
    }

    private Vec3 getOwnerAnchor(LivingEntity owner) {
        Vec3 view = getBladeDirection();
        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(view).normalize();
        return owner.getEyePosition(1.0F).add(view.scale(0.34D))
                .add(right.scale(0.13D)).add(up.scale(0.04D));
    }

    private void spawnWaterParticles(Vec3 center, boolean moving) {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        int fallingCount = moving ? 1 : 2;
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                center.x, center.y, center.z, fallingCount,
                moving ? 0.10D : 0.16D, moving ? 0.16D : 0.20D,
                moving ? 0.10D : 0.16D, 0.01D);
        if ((!moving && tickCount % 6 == 0) || (moving && tickCount % 4 == 0)) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    center.x, center.y, center.z, 1,
                    0.10D, 0.12D, 0.10D, 0.015D);
        }
    }

    public Vec3 getBladeDirection() {
        Vec3 synced = new Vec3(entityData.get(DIRECTION_X),
                entityData.get(DIRECTION_Y), entityData.get(DIRECTION_Z));
        return synced.lengthSqr() < 1.0E-6D ? direction : synced.normalize();
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

    public float getConstructionProgress(float partialTick) {
        // During charge this controls the condensed sword-aura gathering;
        // during flight it controls the one-block-to-four-block growth.
        if (tickCount < CHARGE_TICKS) {
            return Math.max(0.0F, Math.min(1.0F,
                    (tickCount + partialTick) / (float) CHARGE_TICKS));
        }
        return Math.max(0.0F, Math.min(1.0F,
                (tickCount + partialTick - CHARGE_TICKS) / (float) GROW_TICKS));
    }

    public boolean isCharging() {
        return tickCount < CHARGE_TICKS;
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
