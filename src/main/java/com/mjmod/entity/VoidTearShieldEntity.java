package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 二技能蓄力水盾：跟随玩家三秒，随后从盾面喷出多发泪滴。 */
public class VoidTearShieldEntity extends Entity {
    public static final int CHARGE_TICKS = 60;
    /**
     * Keep both normal and Shift defensive shields in front of the player.
     * The old eye-near anchor (0.30 blocks) intersected the player's own
     * collision box while walking; this offset leaves a visible gap without
     * changing the shield dimensions or its firing tracks.
     */
    private static final double SHIELD_FORWARD_OFFSET = 0.95D;
    private static final int FADE_TICKS = 20;
    /** Shift+Z defensive extension: the shield exists for exactly three seconds. */
    public static final int DEFENSIVE_DURATION_TICKS = 60;
    private static final float MAX_ABSORPTION = 500.0F;
    /** 二技能的万泪齐发数量；一技能仍然由 VoidTearSkillManager 发射三道。 */
    private static final int SHOT_COUNT = 24;
    private static final double WALL_HALF_WIDTH = 2.15D;
    private static final double WALL_HALF_HEIGHT = 1.70D;
    private static final double WALL_HALF_DEPTH = 0.24D;
    private static final float WALL_DAMAGE = 10.0F;
    private final Map<UUID, Long> barrierHitCooldown = new HashMap<>();
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FACING_YAW =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FACING_PITCH =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DEFENSIVE_ONLY =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> ABSORPTION =
            SynchedEntityData.defineId(VoidTearShieldEntity.class, EntityDataSerializers.FLOAT);
    private Vec3 firingDirection = new Vec3(0.0D, 0.0D, 1.0D);

    public VoidTearShieldEntity(EntityType<? extends VoidTearShieldEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public VoidTearShieldEntity(ServerLevel level, ServerPlayer owner, Vec3 direction) {
        this(level, owner, direction, false);
    }

    public VoidTearShieldEntity(ServerLevel level, ServerPlayer owner, Vec3 direction,
                                boolean defensiveOnly) {
        this(ModEntities.VOID_TEAR_SHIELD.get(), level);
        entityData.set(OWNER_ID, owner.getId());
        entityData.set(DEFENSIVE_ONLY, defensiveOnly);
        entityData.set(ABSORPTION, MAX_ABSORPTION);
        firingDirection = direction.normalize();
        setFacing(direction);
        setPos(getTearAnchor(owner, firingDirection));
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER_ID, -1);
        entityData.define(PHASE, 0);
        entityData.define(FACING_YAW, 0.0F);
        entityData.define(FACING_PITCH, 0.0F);
        entityData.define(DEFENSIVE_ONLY, false);
        entityData.define(ABSORPTION, MAX_ABSORPTION);
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwnerEntity();
        if (owner != null) {
            // The shield grows from the same right-eye anchor as the helmet and
            // the first skill, instead of appearing at the player's stomach.
            setPos(getTearAnchor(owner, getFacingDirection()));
        }
        if (level().isClientSide) return;

        if (!(owner instanceof ServerPlayer player) || !player.isAlive()
                || !com.mjmod.skill.VoidTearsSkillManager.hasPermission(player)
                || !com.mjmod.skill.VoidTearsSkillManager.isActive(player)) {
            discard();
            return;
        }

        // The visible shield is also a one-sided, invisible entity wall. Mobs
        // touching the front plane are pushed back and receive the same hit
        // logic as every other Void Tears projectile.
        maintainBarrier((ServerLevel) level(), player);

        if (isDefensiveOnly()) {
            // Defensive mode never calls fireShots. It stays as a full shield
            // for three seconds, then uses the same short fade as the normal
            // shield before being removed.
            absorbIncomingProjectiles((ServerLevel) level(), player);
            if (tickCount >= DEFENSIVE_DURATION_TICKS - FADE_TICKS) {
                entityData.set(PHASE, 1);
            }
            if (tickCount >= DEFENSIVE_DURATION_TICKS || getAbsorptionRemaining() <= 0.0F) {
                entityData.set(PHASE, 2);
                discard();
            }
            return;
        }

        if (tickCount == CHARGE_TICKS) {
            fireShots((ServerLevel) level(), player);
            entityData.set(PHASE, 1);
        } else if (tickCount >= CHARGE_TICKS + FADE_TICKS) {
            entityData.set(PHASE, 2);
            discard();
        }
    }

    private void fireShots(ServerLevel level, ServerPlayer player) {
        Vec3 forward = firingDirection.normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        // Irregular fixed launch points on the shield surface. The points are
        // deliberately not a grid, while every projectile keeps the exact same
        // forward direction and therefore never fans outward.
        // Keep every rail on the same forward track, but scatter its root
        // across the shield in a deliberately irregular pattern.  This is
        // not a row/grid: each line starts at a different point on the water
        // shield while retaining the same direction and 35-block reach.
        double[] surfaceX = {
                -1.45D,  0.22D, -0.73D,  1.37D, -0.18D,  0.94D,
                -1.68D,  0.61D, -1.02D,  1.62D, -0.44D,  0.07D,
                 1.13D, -1.25D,  0.39D, -0.91D,  1.75D, -0.06D,
                 0.78D, -1.57D,  0.52D, -0.29D,  1.31D, -0.81D
        };
        double[] surfaceY = {
                 0.83D, -0.21D,  0.14D, -0.72D,  0.56D, -0.05D,
                -0.41D,  0.97D, -0.89D,  0.33D, -0.63D,  0.76D,
                -0.12D,  0.46D, -0.96D,  0.07D,  0.68D, -0.35D,
                 1.01D, -0.58D,  0.25D, -0.79D,  0.91D, -0.02D
        };
        for (int i = 0; i < SHOT_COUNT; i++) {
            // Deliberately irregular launch points on the shield surface; every
            // one still travels exactly along the same forward rail.
            // The shield entity is anchored at the helmet eye model.  Its
            // rendered water surface is only about 0.1 block in front of that
            // anchor; starting 1.65 blocks forward made every rail float far
            // in front of the visible shield.
            Vec3 origin = position().add(forward.scale(0.12D))
                    .add(right.scale(surfaceX[i])).add(up.scale(surfaceY[i]));
            VoidTearShotEntity shot = new VoidTearShotEntity(level, player, origin, forward);
            level.addFreshEntity(shot);
        }
    }

    public boolean isDefensiveOnly() {
        return entityData.get(DEFENSIVE_ONLY);
    }

    public float getAbsorptionRemaining() {
        return Math.max(0.0F, entityData.get(ABSORPTION));
    }

    /** Returns how much of the incoming damage this shield absorbed. */
    public float absorbDamage(float amount) {
        if (!isDefensiveOnly() || amount <= 0.0F || !isAlive()) return 0.0F;
        float available = getAbsorptionRemaining();
        float absorbed = Math.min(available, amount);
        entityData.set(ABSORPTION, available - absorbed);
        return absorbed;
    }

    /**
     * Removes projectiles that physically reach the defensive curtain. This is
     * intentionally broad: vanilla arrows and every Projectile subclass (thus
     * mod/NBT projectiles) are swallowed, while this mod's own Void Tears rails
     * are ignored so the ordinary skill-2 behaviour cannot interfere.
     */
    private void absorbIncomingProjectiles(ServerLevel level, ServerPlayer owner) {
        if (getAbsorptionRemaining() <= 0.0F) return;
        AABB barrier = getBarrierBox();
        for (Entity incoming : level.getEntities(this, barrier.inflate(1.35D),
                entity -> isAbsorbableIncoming(entity, owner))) {
            float cost = projectileAbsorptionCost(incoming);
            float absorbed = absorbDamage(cost);
            if (absorbed > 0.0F) {
                incoming.discard();
            }
            if (getAbsorptionRemaining() <= 0.0F) break;
        }
    }

    private boolean isAbsorbableIncoming(Entity incoming, ServerPlayer owner) {
        if (incoming == owner || incoming == this || !incoming.isAlive()) return false;
        if (incoming instanceof VoidTearBeamEntity || incoming instanceof VoidTearShotEntity
                || incoming instanceof VoidTearShieldEntity) return false;
        if (incoming instanceof Projectile projectile) {
            return projectile.getOwner() != owner;
        }
        return incoming instanceof ItemEntity item && isArrowItem(item.getItem());
    }

    private static boolean isArrowItem(ItemStack stack) {
        return stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW)
                || stack.is(Items.TIPPED_ARROW);
    }

    private static float projectileAbsorptionCost(Entity incoming) {
        if (incoming instanceof AbstractArrow arrow) {
            return Math.max(1.0F, (float) arrow.getBaseDamage());
        }
        if (incoming instanceof ItemEntity item) {
            return Math.max(1.0F, item.getItem().getCount());
        }
        // Custom NBT projectiles do not share a damage API. A conservative one
        // point cost still lets the shield swallow them without assuming their
        // mod-specific damage format.
        return 1.0F;
    }

    /**
     * Called from LivingHurtEvent as a fallback for projectiles that hit the
     * player between server ticks. Returns the damage left after absorption.
     */
    public static float absorbProjectileDamage(ServerPlayer target, DamageSource source,
                                                float amount) {
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        if (!(direct instanceof Projectile) && !(owner instanceof Projectile)) return amount;

        AABB search = target.getBoundingBox().inflate(8.0D);
        for (VoidTearShieldEntity shield : target.serverLevel().getEntitiesOfClass(
                VoidTearShieldEntity.class, search,
                candidate -> candidate.isDefensiveOnly()
                        && candidate.getOwnerEntity() == target
                        && candidate.isAlive())) {
            float absorbed = shield.absorbDamage(amount);
            if (absorbed <= 0.0F) continue;
            if (absorbed >= amount) {
                Entity projectile = direct instanceof Projectile ? direct
                        : owner instanceof Projectile ? owner : null;
                if (projectile != null) projectile.discard();
            }
            return amount - absorbed;
        }
        return amount;
    }

    private Entity getOwnerEntity() {
        int id = entityData.get(OWNER_ID);
        return id >= 0 ? level().getEntity(id) : null;
    }

    private void setFacing(Vec3 direction) {
        Vec3 normalized = direction.normalize();
        entityData.set(FACING_YAW, (float) Math.atan2(normalized.x, normalized.z));
        entityData.set(FACING_PITCH, (float) Math.atan2(normalized.y,
                Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)));
    }

    public Vec3 getFacingDirection() {
        float yaw = entityData.get(FACING_YAW);
        float pitch = entityData.get(FACING_PITCH);
        float horizontal = (float) Math.cos(pitch);
        return new Vec3(Math.sin(yaw) * horizontal, Math.sin(pitch),
                Math.cos(yaw) * horizontal).normalize();
    }

    private void maintainBarrier(ServerLevel level, ServerPlayer owner) {
        if (getPhase() > 1) return;

        Vec3 facing = getFacingDirection();
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 right = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        // Match the invisible collision plane to the visible water curtain;
        // mobs should be stopped as soon as they touch the shield surface.
        Vec3 center = position().add(facing.scale(0.20D));
        double extentX = Math.abs(right.x) * WALL_HALF_WIDTH
                + Math.abs(horizontal.x) * WALL_HALF_DEPTH + 0.45D;
        double extentZ = Math.abs(right.z) * WALL_HALF_WIDTH
                + Math.abs(horizontal.z) * WALL_HALF_DEPTH + 0.45D;
        AABB barrier = new AABB(center.x - extentX, center.y - WALL_HALF_HEIGHT,
                center.z - extentZ, center.x + extentX, center.y + WALL_HALF_HEIGHT,
                center.z + extentZ);

        long now = level.getGameTime();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, barrier,
                entity -> entity instanceof Mob && entity != owner && entity.isAlive()
                        && !entity.isSpectator())) {
            Vec3 relative = target.position().subtract(center);
            double lateral = relative.dot(right);
            if (Math.abs(lateral) > WALL_HALF_WIDTH + target.getBbWidth() * 0.5D) continue;
            if (target.getBoundingBox().maxY < center.y - WALL_HALF_HEIGHT
                    || target.getBoundingBox().minY > center.y + WALL_HALF_HEIGHT) continue;

            double depth = relative.dot(horizontal);
            double side = depth >= 0.0D ? 1.0D : -1.0D;
            double safeDepth = WALL_HALF_DEPTH + target.getBbWidth() * 0.5D + 0.08D;
            if (Math.abs(depth) < safeDepth) {
                double correction = side * safeDepth - depth;
                target.move(MoverType.SELF, horizontal.scale(correction));
                Vec3 motion = target.getDeltaMovement();
                if (motion.dot(horizontal) * side < 0.0D) {
                    target.setDeltaMovement(motion.subtract(horizontal.scale(motion.dot(horizontal))));
                }
            }

            Long nextHit = barrierHitCooldown.get(target.getUUID());
            if (nextHit == null || now >= nextHit) {
                double hitX = Math.max(target.getBoundingBox().minX,
                        Math.min(center.x, target.getBoundingBox().maxX));
                double hitY = Math.max(target.getBoundingBox().minY,
                        Math.min(center.y, target.getBoundingBox().maxY));
                double hitZ = Math.max(target.getBoundingBox().minZ,
                        Math.min(center.z, target.getBoundingBox().maxZ));
                target.invulnerableTime = 0;
                com.mjmod.skill.VoidTearsSkillManager.applyVoidTearHit(level, owner, target,
                        new EntityHitResult(target, new Vec3(hitX, hitY, hitZ)), WALL_DAMAGE);
                target.invulnerableTime = 0;
                barrierHitCooldown.put(target.getUUID(), now + 10L);
            }
        }
        if (barrierHitCooldown.size() > 128) barrierHitCooldown.clear();
    }

    /** Same collision curtain used by both normal and defensive shield modes. */
    private AABB getBarrierBox() {
        Vec3 facing = getFacingDirection();
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 right = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        Vec3 center = position().add(facing.scale(0.20D));
        double extentX = Math.abs(right.x) * WALL_HALF_WIDTH
                + Math.abs(horizontal.x) * WALL_HALF_DEPTH + 0.45D;
        double extentZ = Math.abs(right.z) * WALL_HALF_WIDTH
                + Math.abs(horizontal.z) * WALL_HALF_DEPTH + 0.45D;
        return new AABB(center.x - extentX, center.y - WALL_HALF_HEIGHT,
                center.z - extentZ, center.x + extentX, center.y + WALL_HALF_HEIGHT,
                center.z + extentZ);
    }

    private static Vec3 getTearAnchor(Entity owner, Vec3 view) {
        view = view.normalize();
        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(view).normalize();
        return owner.getEyePosition(1.0F)
                .add(view.scale(SHIELD_FORWARD_OFFSET))
                .add(right.scale(0.13D))
                .add(up.scale(0.04D));
    }

    public float getChargeProgress(float partialTick) {
        if (isDefensiveOnly()) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F,
                (tickCount + partialTick) / (float) CHARGE_TICKS));
    }

    public float getFadeAlpha(float partialTick) {
        if (entityData.get(PHASE) == 0) return 1.0F;
        int fadeStart = isDefensiveOnly()
                ? DEFENSIVE_DURATION_TICKS - FADE_TICKS : CHARGE_TICKS;
        float progress = (tickCount + partialTick - fadeStart) / (float) FADE_TICKS;
        return Math.max(0.0F, 1.0F - progress);
    }

    public int getPhase() {
        return entityData.get(PHASE);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        entityData.set(OWNER_ID, tag.getInt("OwnerId"));
        entityData.set(PHASE, tag.getInt("Phase"));
        entityData.set(FACING_YAW, tag.getFloat("FacingYaw"));
        entityData.set(FACING_PITCH, tag.getFloat("FacingPitch"));
        entityData.set(DEFENSIVE_ONLY, tag.getBoolean("DefensiveOnly"));
        entityData.set(ABSORPTION, tag.contains("Absorption")
                ? tag.getFloat("Absorption") : MAX_ABSORPTION);
        firingDirection = getFacingDirection();
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putInt("OwnerId", entityData.get(OWNER_ID));
        tag.putInt("Phase", entityData.get(PHASE));
        tag.putFloat("FacingYaw", entityData.get(FACING_YAW));
        tag.putFloat("FacingPitch", entityData.get(FACING_PITCH));
        tag.putBoolean("DefensiveOnly", entityData.get(DEFENSIVE_ONLY));
        tag.putFloat("Absorption", entityData.get(ABSORPTION));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
