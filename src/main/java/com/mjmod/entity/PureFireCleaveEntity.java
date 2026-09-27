package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 六技能【横斩】（Wide Cleave）实体控制器：
 * 时长 24 tick（1.0 秒），对齐“横斩as老冯”动画。
 * 伤害：固定 10 点。
 * 判定盒：长度 5 格（前方深度）、宽度 3 格（左右各 1.5 格）、高度 1.8 格。
 * 在挥刀帧在玩家身前 1~2 格斩出月牙形半透明纯阳气浪扫过范围，附带细碎纯阳火星爆裂。
 */
public final class PureFireCleaveEntity extends Entity {
    public static final int DURATION_TICKS = 24;
    public static final int SLASH_START_TICK = 9;
    public static final int SLASH_END_TICK = 15;
    public static final float BASE_DAMAGE = 10.0F;

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireCleaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireCleaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFireCleaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireCleaveEntity.class, EntityDataSerializers.FLOAT);

    private final Set<Integer> hitEntityIds = new HashSet<>();
    private boolean slashSoundPlayed = false;

    public PureFireCleaveEntity(EntityType<? extends PureFireCleaveEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireCleaveEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_CLEAVE.get(), level);
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
        entityData.define(DIR_X, 0.0F);
        entityData.define(DIR_Y, 0.0F);
        entityData.define(DIR_Z, 1.0F);
    }

    public LivingEntity getOwner() {
        Entity e = level().getEntity(entityData.get(OWNER_ID));
        return e instanceof LivingEntity living ? living : null;
    }

    public Vec3 getCleaveDirection() {
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
            if (tickCount == SLASH_START_TICK && !slashSoundPlayed) {
                slashSoundPlayed = true;
                serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.9F);
                serverLevel.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
            }

            if (tickCount >= SLASH_START_TICK && tickCount <= SLASH_END_TICK) {
                performSlashAttack(serverLevel, owner);
            }

            if (tickCount >= DURATION_TICKS) {
                discard();
            }
        }
    }

    private void performSlashAttack(ServerLevel level, LivingEntity owner) {
        Vec3 forward = getCleaveDirection();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 1.0E-5D) right = new Vec3(1, 0, 0);

        // Progress of the sweep from right to left (0.0 to 1.0)
        float sweepT = (tickCount - SLASH_START_TICK) / (float) (SLASH_END_TICK - SLASH_START_TICK);
        double angleSweep = (sweepT - 0.5D) * Math.PI * 0.85D; // -76 to +76 deg sweep
        Vec3 sweepDir = forward.scale(Math.cos(angleSweep)).add(right.scale(Math.sin(angleSweep))).normalize();

        // 强化纯粒子斩击弧光：生成紧密、高密度的纯阳粒子与火花
        for (double dist = 0.8D; dist <= 5.0D; dist += 0.25D) {
            Vec3 basePos = owner.getEyePosition().add(0, -0.2D, 0).add(sweepDir.scale(dist));
            // 主弧线粒子
            level.sendParticles(ModParticles.PURE_FIRE.get(), basePos.x, basePos.y, basePos.z,
                    3, 0.08D, 0.06D, 0.08D, 0.015D);

            // 外围飞溅纯阳火花
            if (random.nextFloat() < 0.45F) {
                level.sendParticles(ModParticles.PURE_FIRE.get(), basePos.x, basePos.y + 0.1D, basePos.z,
                        1, 0.15D, 0.12D, 0.15D, 0.04D);
            }
        }

        // Hitbox: Forward depth 5 blocks, Width 3 blocks (left/right 1.5 blocks), Height 1.8 blocks
        Vec3 center = owner.position().add(forward.scale(2.5D)).add(0, 0.9D, 0);
        AABB box = new AABB(center.x - 2.5D, center.y - 1.0D, center.z - 2.5D,
                center.x + 2.5D, center.y + 1.0D, center.z + 2.5D);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != owner && e.isAlive() && !e.isAlliedTo(owner));

        for (LivingEntity target : targets) {
            if (hitEntityIds.contains(target.getId())) continue;

            // Check relative position: depth along forward 0..5, width along right -1.5..1.5, height -0.2..2.0
            Vec3 toTarget = target.position().subtract(owner.position());
            double depth = toTarget.dot(forward);
            double lateral = Math.abs(toTarget.dot(right));
            double heightDiff = target.getY() - owner.getY();

            if (depth >= 0.2D && depth <= 5.2D && lateral <= 1.8D && heightDiff >= -0.8D && heightDiff <= 2.2D) {
                hitEntityIds.add(target.getId());
                DamageSource ds = owner instanceof ServerPlayer sp
                        ? level.damageSources().playerAttack(sp)
                        : level.damageSources().mobAttack(owner);
                target.hurt(ds, com.mjmod.skill.PureFireSkillManager.modifyPureFireDamage(owner, BASE_DAMAGE));
                target.setSecondsOnFire(3);

                // Pure fire spark burst on hit
                level.sendParticles(ModParticles.PURE_FIRE.get(),
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        12, 0.25D, 0.25D, 0.25D, 0.08D);
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BLAZE_HURT, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("OwnerId")) entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        if (tag.contains("DirX")) entityData.set(DIR_X, tag.getFloat("DirX"));
        if (tag.contains("DirY")) entityData.set(DIR_Y, tag.getFloat("DirY"));
        if (tag.contains("DirZ")) entityData.set(DIR_Z, tag.getFloat("DirZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putFloat("DirX", entityData.get(DIR_X));
        tag.putFloat("DirY", entityData.get(DIR_Y));
        tag.putFloat("DirZ", entityData.get(DIR_Z));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
