package com.mjmod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 厄喙兽：仅注册实体类型，不加入任何生物群系的自然生成表。 */
public class EHuiShouEntity extends Monster {
    private static final int CHARGE_INTERVAL_TICKS = 200;
    private static final int CHARGE_DURATION_TICKS = 10;
    private static final double CHARGE_DISTANCE = 8.0D;
    private static final double CHARGE_STEP = CHARGE_DISTANCE / CHARGE_DURATION_TICKS;
    private static final float CHARGE_DAMAGE = 15.0F;
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(EHuiShouEntity.class, EntityDataSerializers.BOOLEAN);

    private int chargeCooldown = CHARGE_INTERVAL_TICKS;
    private int chargeTicksRemaining;
    private Vec3 chargeDirection = Vec3.ZERO;
    private final Set<UUID> chargeHitEntities = new HashSet<>();

    public EHuiShouEntity(EntityType<? extends EHuiShouEntity> type, Level level) {
        super(type, level);
        this.xpReward = 18;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2,
                new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CHARGING, false);
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) return;

        if (isCharging()) {
            tickCharge();
            return;
        }

        if (chargeCooldown > 0) chargeCooldown--;
        LivingEntity target = getTarget();
        if (chargeCooldown <= 0 && target != null && target.isAlive()
                && distanceToSqr(target) <= 32.0D * 32.0D) {
            beginCharge(target);
        }
    }

    private void beginCharge(LivingEntity target) {
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 direction = targetCenter.subtract(position());
        if (direction.lengthSqr() < 1.0E-5D) direction = getLookAngle();
        chargeDirection = direction.normalize();
        chargeTicksRemaining = CHARGE_DURATION_TICKS;
        chargeHitEntities.clear();
        entityData.set(CHARGING, true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        setYRot((float) (Mth.atan2(-chargeDirection.x, chargeDirection.z) * Mth.RAD_TO_DEG));
        yRotO = getYRot();
    }

    private void tickCharge() {
        if (chargeTicksRemaining <= 0) {
            finishCharge();
            return;
        }

        // Keep melee/navigation goals from adding a second movement vector
        // while the timed dash owns the entity's position.
        getNavigation().stop();
        Vec3 step = chargeDirection.scale(CHARGE_STEP);
        AABB swept = getBoundingBox().expandTowards(step).inflate(0.2D);
        breakChargeBlocks(swept);

        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, swept,
                entity -> entity != this && entity.isAlive() && !entity.isSpectator())) {
            if (chargeHitEntities.add(target.getUUID())) {
                target.invulnerableTime = 0;
                target.hurt(damageSources().mobAttack(this), CHARGE_DAMAGE);
                target.knockback(0.45D, -chargeDirection.x, -chargeDirection.z);
            }
        }

        setDeltaMovement(step);
        move(MoverType.SELF, step);
        setDeltaMovement(Vec3.ZERO);
        chargeTicksRemaining--;
        if (chargeTicksRemaining <= 0) finishCharge();
    }

    private void breakChargeBlocks(AABB area) {
        if (!(level() instanceof ServerLevel serverLevel)
                || !serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;

        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    if (!serverLevel.getBlockState(cursor).isAir()) {
                        serverLevel.destroyBlock(cursor, true, this);
                    }
                }
            }
        }
    }

    private void finishCharge() {
        entityData.set(CHARGING, false);
        chargeCooldown = CHARGE_INTERVAL_TICKS;
        chargeDirection = Vec3.ZERO;
        chargeHitEntities.clear();
        setDeltaMovement(Vec3.ZERO);
    }

    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return super.doHurtTarget(target);
    }
}
