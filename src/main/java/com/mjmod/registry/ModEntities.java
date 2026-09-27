package com.mjmod.registry;

import com.mjmod.MJMod;
import com.mjmod.entity.VoidTearBeamEntity;
import com.mjmod.entity.VoidTearShieldEntity;
import com.mjmod.entity.VoidTearShotEntity;
import com.mjmod.entity.VoidTearBladeEntity;
import com.mjmod.entity.VoidTearDropEntity;
import com.mjmod.entity.EHuiShouEntity;
import com.mjmod.entity.ShenQingShuiZhiYaoEntity;
import com.mjmod.entity.OrientalClanEntity;
import com.mjmod.entity.PureFireVortexEntity;
import com.mjmod.entity.PureFirePunchEntity;
import com.mjmod.entity.PureFireMeteorDiveEntity;
import com.mjmod.entity.PureFireCleaveEntity;
import com.mjmod.entity.PureFireThrustEntity;
import com.mjmod.entity.PureFireDomainEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体注册表
 */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MJMod.MODID);

    public static final RegistryObject<EntityType<VoidTearBeamEntity>> VOID_TEAR_PROJECTILE =
            ENTITIES.register("void_tear_projectile",
                    () -> EntityType.Builder.<VoidTearBeamEntity>of(VoidTearBeamEntity::new, MobCategory.MISC)
                            .sized(0.18F, 0.18F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("void_tear_projectile"));

    public static final RegistryObject<EntityType<VoidTearShieldEntity>> VOID_TEAR_SHIELD =
            ENTITIES.register("void_tear_shield",
                    () -> EntityType.Builder.<VoidTearShieldEntity>of(VoidTearShieldEntity::new, MobCategory.MISC)
                            .sized(4.5F, 4.5F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("void_tear_shield"));

    public static final RegistryObject<EntityType<VoidTearShotEntity>> VOID_TEAR_SHOT =
            ENTITIES.register("void_tear_shot",
                    () -> EntityType.Builder.<VoidTearShotEntity>of(VoidTearShotEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("void_tear_shot"));

    /** 泪刃：60 格竖向斩击实体 */
    public static final RegistryObject<EntityType<VoidTearBladeEntity>> VOID_TEAR_BLADE =
            ENTITIES.register("void_tear_blade",
                    () -> EntityType.Builder.<VoidTearBladeEntity>of(VoidTearBladeEntity::new, MobCategory.MISC)
                            .sized(0.6F, 6.0F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("void_tear_blade"));

    /** 第五技能：玩家背后的自动护卫虚空水珠。 */
    public static final RegistryObject<EntityType<VoidTearDropEntity>> VOID_TEAR_DROP =
            ENTITIES.register("void_tear_drop",
                    () -> EntityType.Builder.<VoidTearDropEntity>of(
                                    VoidTearDropEntity::new, MobCategory.MISC)
                            .sized(0.42F, 0.42F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("void_tear_drop"));

    /** 纯质阳炎一技能：服务端同步的实体火焰漩涡。 */
    public static final RegistryObject<EntityType<PureFireVortexEntity>> PURE_FIRE_VORTEX =
            ENTITIES.register("pure_fire_vortex",
                    () -> EntityType.Builder.<PureFireVortexEntity>of(PureFireVortexEntity::new, MobCategory.MISC)
                            .sized(0.7F, 0.7F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("pure_fire_vortex"));

    /** 纯质阳炎三技能：玩家右拳直冲动画与服务端命中控制器。 */
    public static final RegistryObject<EntityType<PureFirePunchEntity>> PURE_FIRE_PUNCH =
            ENTITIES.register("pure_fire_punch",
                    () -> EntityType.Builder.<PureFirePunchEntity>of(
                                    PureFirePunchEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("pure_fire_punch"));

    /** 纯质阳炎四技能：跃起十格后向准心落点爆冲下坠。 */
    public static final RegistryObject<EntityType<PureFireMeteorDiveEntity>> PURE_FIRE_METEOR_DIVE =
            ENTITIES.register("pure_fire_meteor_dive",
                    () -> EntityType.Builder.<PureFireMeteorDiveEntity>of(
                                    PureFireMeteorDiveEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_meteor_dive"));

    /** 纯质阳炎六技能：横斩断魂。 */
    public static final RegistryObject<EntityType<PureFireCleaveEntity>> PURE_FIRE_CLEAVE =
            ENTITIES.register("pure_fire_cleave",
                    () -> EntityType.Builder.<PureFireCleaveEntity>of(
                                    PureFireCleaveEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_cleave"));

    /** 纯质阳炎七技能：穿刺（瞬影贯心）。 */
    public static final RegistryObject<EntityType<PureFireThrustEntity>> PURE_FIRE_THRUST =
            ENTITIES.register("pure_fire_thrust",
                    () -> EntityType.Builder.<PureFireThrustEntity>of(
                                    PureFireThrustEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_thrust"));

    /** 纯质阳炎八技能：火墙（横向火幕）。 */
    public static final RegistryObject<EntityType<com.mjmod.entity.PureFireWallEntity>> PURE_FIRE_WALL =
            ENTITIES.register("pure_fire_wall",
                    () -> EntityType.Builder.<com.mjmod.entity.PureFireWallEntity>of(
                                    com.mjmod.entity.PureFireWallEntity::new, MobCategory.MISC)
                            .sized(com.mjmod.entity.PureFireWallEntity.WALL_WIDTH, com.mjmod.entity.PureFireWallEntity.WALL_HEIGHT)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_wall"));

    /** 纯质阳炎八技能：火墙施法实体（动作与前摇控制）。 */
    public static final RegistryObject<EntityType<com.mjmod.entity.PureFireWallCastEntity>> PURE_FIRE_WALL_CAST =
            ENTITIES.register("pure_fire_wall_cast",
                    () -> EntityType.Builder.<com.mjmod.entity.PureFireWallCastEntity>of(
                                    com.mjmod.entity.PureFireWallCastEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_wall_cast"));

    /** 纯质阳炎：烈阳火球（支持蓄力投掷与环绕护体）。 */
    public static final RegistryObject<EntityType<com.mjmod.entity.PureFireBallEntity>> PURE_FIRE_BALL =
            ENTITIES.register("pure_fire_ball",
                    () -> EntityType.Builder.<com.mjmod.entity.PureFireBallEntity>of(
                                    com.mjmod.entity.PureFireBallEntity::new, MobCategory.MISC)
                            .sized(0.7F, 0.7F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .setShouldReceiveVelocityUpdates(true)
                            .build("pure_fire_ball"));

    /** 纯质阳炎：烈阳火球施法控制器实体（蓄力与下砸动作驱动）。 */
    public static final RegistryObject<EntityType<com.mjmod.entity.PureFireBallCastEntity>> PURE_FIRE_BALL_CAST =
            ENTITIES.register("pure_fire_ball_cast",
                    () -> EntityType.Builder.<com.mjmod.entity.PureFireBallCastEntity>of(
                                    com.mjmod.entity.PureFireBallCastEntity::new, MobCategory.MISC)
                            .sized(0.7F, 1.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("pure_fire_ball_cast"));

    /** 纯质阳炎十技能：固定中心的阳炎领域逻辑锚点。 */
    public static final RegistryObject<EntityType<PureFireDomainEntity>> PURE_FIRE_DOMAIN =
            ENTITIES.register("pure_fire_domain",
                    () -> EntityType.Builder.<PureFireDomainEntity>of(
                                    PureFireDomainEntity::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("pure_fire_domain"));

    /** 厄喙兽仅能通过命令等显式方式生成，不注册自然生成规则。 */
    public static final RegistryObject<EntityType<EHuiShouEntity>> EHUI_SHOU =
            ENTITIES.register("ehuishou",
                    () -> EntityType.Builder.of(EHuiShouEntity::new, MobCategory.MONSTER)
                            // The scaled model spans about 10 blocks along
                            // its longest horizontal axis and 4 blocks high.
                            .sized(10.0F, 4.0F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("ehuishou"));

    /** 某不知名深情水蛭妖：友好 NPC，在河流群系岸边低概率生成。 */
    public static final RegistryObject<EntityType<ShenQingShuiZhiYaoEntity>> SHEN_QING_SHUI_ZHI_YAO =
            ENTITIES.register("shenqing_shuizhiyao",
                    () -> EntityType.Builder.of(ShenQingShuiZhiYaoEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(48)
                            .updateInterval(2)
                            .build("shenqing_shuizhiyao"));

    /** 游荡的东方一族：中立战士，在平原群系低概率生成。 */
    public static final RegistryObject<EntityType<OrientalClanEntity>> ORIENTAL_CLAN =
            ENTITIES.register("oriental_clan",
                    () -> EntityType.Builder.of(OrientalClanEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(48)
                            .updateInterval(2)
                            .build("oriental_clan"));
}
