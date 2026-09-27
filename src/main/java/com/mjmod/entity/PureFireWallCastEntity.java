package com.mjmod.entity;

import com.mjmod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 纯质阳炎八技能【火墙】施法控制器实体（与玩家绑定）：
 * 总时长 26 ticks（1.29 秒，按 20tick/s 计算 1.29s 约为 26 ticks，30fps下对应 39 帧）。
 * 0.40 秒（第 8 tick）触发在施法者前方地表生成 PureFireWallEntity。
 * 供客户端 Player Animator 捕获并在施法期间播放专属动画动作。
 */
public final class PureFireWallCastEntity extends Entity {
    public static final int DURATION_TICKS = 26; // 1.29s (26 ticks)
    public static final int WALL_SPAWN_TICK = 8; // 0.40s (8 ticks)
    public static final double SPAWN_FORWARD_DISTANCE = 3.5D; // 在前方 3.5 格处生成

    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
            PureFireWallCastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DIR_X = SynchedEntityData.defineId(
            PureFireWallCastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Y = SynchedEntityData.defineId(
            PureFireWallCastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR_Z = SynchedEntityData.defineId(
            PureFireWallCastEntity.class, EntityDataSerializers.FLOAT);

    private boolean wallSpawned = false;

    public PureFireWallCastEntity(EntityType<? extends PureFireWallCastEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public PureFireWallCastEntity(ServerLevel level, ServerPlayer owner) {
        this(ModEntities.PURE_FIRE_WALL_CAST.get(), level);
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
        Entity entity = level().getEntity(entityData.get(OWNER_ID));
        return entity instanceof LivingEntity living ? living : null;
    }

    public Vec3 getCastDirection() {
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

        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            // 第 8 tick（0.4s）正式释放生成火墙
            if (tickCount >= WALL_SPAWN_TICK && !wallSpawned) {
                wallSpawned = true;
                Vec3 look = getCastDirection();
                Vec3 groundPos = PureFireWallEntity.findGroundSpawnPos(sl, owner, SPAWN_FORWARD_DISTANCE);
                PureFireWallEntity wall = new PureFireWallEntity(sl, owner, groundPos, look);
                sl.addFreshEntity(wall);
            }

            if (tickCount >= DURATION_TICKS) {
                if (owner instanceof ServerPlayer player) {
                    com.mjmod.skill.PureFireSkillManager.holdFireBlade(player);
                }
                discard();
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
