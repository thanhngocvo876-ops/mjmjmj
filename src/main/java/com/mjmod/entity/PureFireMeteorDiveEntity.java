package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Server-authoritative controller for pure-fire skill 4: leap ten blocks,
 * then meteor-dive toward the activation crosshair and detonate on impact.
 */
public final class PureFireMeteorDiveEntity extends Entity {
    public static final int ASCENT_TICKS = 20;
    public static final int DIVE_TICKS = 15;
    public static final int DURATION_TICKS = ASCENT_TICKS + DIVE_TICKS;
    public static final double RISE_HEIGHT = 10.0D;
    public static final double IMPACT_HALF_SIZE = 2.0D;
    public static final float IMPACT_DAMAGE = 20.0F;
    private static final double TARGET_RANGE = 32.0D;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireMeteorDiveEntity.class, EntityDataSerializers.INT);

    private Vec3 startPosition;
    private Vec3 apexPosition;
    private Vec3 targetPosition;
    private boolean impacted;

    public PureFireMeteorDiveEntity(EntityType<? extends PureFireMeteorDiveEntity> type,
                                    Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireMeteorDiveEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_METEOR_DIVE.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        startPosition = owner.position();
        targetPosition = resolveCrosshairTarget(level, owner);
        double apexY = Math.min(level.getMaxBuildHeight() - 2.0D,
                startPosition.y + RISE_HEIGHT);
        apexPosition = new Vec3(startPosition.x, apexY, startPosition.z);
        setPos(startPosition);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
    }

    public LivingEntity getOwner() {
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    /** 0 = one-second ascent, 1 = 0.75-second dive, 2 = finished. */
    public int getPhase() {
        if (tickCount <= ASCENT_TICKS) return 0;
        if (tickCount <= DURATION_TICKS) return 1;
        return 2;
    }

    public float getAnimationTimeSeconds(float partialTick) {
        float time = tickCount + partialTick;
        if (time <= ASCENT_TICKS) {
            return Mth.clamp(time / ASCENT_TICKS, 0.0F, 1.0F);
        }
        float dive = Mth.clamp((time - ASCENT_TICKS) / DIVE_TICKS, 0.0F, 1.0F);
        return 1.0F + dive * 0.79167F;
    }

    public float getPhaseProgress(float partialTick) {
        float time = tickCount + partialTick;
        if (time <= ASCENT_TICKS) {
            return Mth.clamp(time / ASCENT_TICKS, 0.0F, 1.0F);
        }
        return Mth.clamp((time - ASCENT_TICKS) / DIVE_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }
        if (level().isClientSide) return;

        ServerLevel server = (ServerLevel) level();
        ensureTrajectory(server, owner);
        owner.fallDistance = 0.0F;
        owner.setDeltaMovement(Vec3.ZERO);

        if (tickCount <= ASCENT_TICKS) {
            float raw = Mth.clamp(tickCount / (float) ASCENT_TICKS, 0.0F, 1.0F);
            float fastEaseOut = 1.0F - (float) Math.pow(1.0F - raw, 3.0D);
            moveOwner(owner, startPosition.lerp(apexPosition, fastEaseOut));
            spawnAscentFlames(server, owner);
            if (tickCount == 1) {
                server.playSound(null, owner.blockPosition(), SoundEvents.FIRECHARGE_USE,
                        SoundSource.PLAYERS, 1.2F, 1.35F);
            }
            return;
        }

        if (tickCount <= DURATION_TICKS) {
            float raw = Mth.clamp((tickCount - ASCENT_TICKS) / (float) DIVE_TICKS,
                    0.0F, 1.0F);
            float violentAcceleration = raw * raw * raw;
            moveOwner(owner, apexPosition.lerp(targetPosition, violentAcceleration));
            spawnDiveFlames(server, owner);
            if (tickCount == ASCENT_TICKS + 1) {
                server.playSound(null, owner.blockPosition(), SoundEvents.BLAZE_SHOOT,
                        SoundSource.PLAYERS, 1.4F, 0.62F);
            }
            if (tickCount == DURATION_TICKS) impact(server, owner);
            return;
        }

        discard();
    }

    private void ensureTrajectory(ServerLevel level, LivingEntity owner) {
        if (startPosition != null && apexPosition != null && targetPosition != null) return;
        startPosition = owner.position();
        targetPosition = owner.position();
        apexPosition = owner.position().add(0.0D, RISE_HEIGHT, 0.0D);
    }

    private void moveOwner(LivingEntity owner, Vec3 position) {
        owner.teleportTo(position.x, position.y, position.z);
        owner.setDeltaMovement(Vec3.ZERO);
        owner.fallDistance = 0.0F;
        setPos(position);
    }

    private static Vec3 resolveCrosshairTarget(ServerLevel level, ServerPlayer owner) {
        Vec3 eye = owner.getEyePosition();
        Vec3 view = owner.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(view.scale(TARGET_RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, owner));
        Vec3 horizontalTarget = hit.getType() == HitResult.Type.MISS
                ? eye.add(view.scale(16.0D)) : hit.getLocation();
        int x = Mth.floor(horizontalTarget.x);
        int z = Mth.floor(horizontalTarget.z);
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(horizontalTarget.x, groundY + 0.05D, horizontalTarget.z);
    }

    private static void spawnAscentFlames(ServerLevel level, LivingEntity owner) {
        Vec3 center = owner.position().add(0.0D, owner.getBbHeight() * 0.52D, 0.0D);
        level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y, center.z,
                14, 0.40D, 0.72D, 0.40D, 0.045D);
        level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y - 0.85D, center.z,
                8, 0.24D, 0.30D, 0.24D, 0.025D);
    }

    private static void spawnDiveFlames(ServerLevel level, LivingEntity owner) {
        Vec3 center = owner.position().add(0.0D, owner.getBbHeight() * 0.50D, 0.0D);
        // Dense enough to surround the entire player silhouette during descent.
        level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y, center.z,
                60, 0.68D, 1.02D, 0.68D, 0.085D);
        level.sendParticles(ModParticles.PURE_FIRE.get(), center.x, center.y + 1.15D, center.z,
                24, 0.38D, 0.72D, 0.38D, 0.045D);
    }

    private void impact(ServerLevel level, LivingEntity owner) {
        if (impacted) return;
        impacted = true;
        moveOwner(owner, targetPosition);
        AABB area = new AABB(
                targetPosition.x - IMPACT_HALF_SIZE, targetPosition.y - 1.0D,
                targetPosition.z - IMPACT_HALF_SIZE,
                targetPosition.x + IMPACT_HALF_SIZE, targetPosition.y + 3.0D,
                targetPosition.z + IMPACT_HALF_SIZE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != owner && entity.isAlive() && !entity.isSpectator())) {
            target.hurt(owner.damageSources().mobAttack(owner),
                    com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, IMPACT_DAMAGE));
            target.setSecondsOnFire(6);
            Vec3 away = target.position().subtract(targetPosition);
            if (away.lengthSqr() > 1.0E-5D) away = away.normalize();
            target.push(away.x * 0.85D, 0.55D, away.z * 0.85D);
        }
        level.sendParticles(ModParticles.PURE_FIRE.get(), targetPosition.x,
                targetPosition.y + 0.55D, targetPosition.z,
                180, 1.85D, 0.95D, 1.85D, 0.13D);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, targetPosition.x,
                targetPosition.y + 0.35D, targetPosition.z,
                4, 1.35D, 0.45D, 1.35D, 0.05D);
        level.sendParticles(ParticleTypes.FLAME, targetPosition.x,
                targetPosition.y + 0.45D, targetPosition.z,
                80, 1.85D, 0.75D, 1.85D, 0.08D);
        level.playSound(null, BlockPos.containing(targetPosition), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.6F, 0.72F);
        owner.fallDistance = 0.0F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
