package com.mjmod.client;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearCastAnimationModel;
import com.mjmod.client.renderer.PureFireHandsLayer;
import com.mjmod.entity.PureFireVortexEntity;
import com.mjmod.entity.PureFirePunchEntity;
import com.mjmod.entity.PureFireMeteorDiveEntity;
import com.mjmod.entity.PureFireCleaveEntity;
import com.mjmod.entity.PureFireThrustEntity;
import com.mjmod.skill.PureFireSkillManager;
import com.mjmod.network.MJNetwork;
import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModParticles;
import com.mjmod.skill.VoidTearsSkillManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Client-only presentation and controls for the Void Tears system. */
@Mod.EventBusSubscriber(modid = MJMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}


    public static boolean VOID_TEARS_ACTIVE = false;
    public static int VOID_TEARS_SELECTED_SKILL = 1;
    public static List<VoidTearsSkillManager.CoordinatePoint> VOID_TEARS_POINTS =
            Collections.emptyList();
    public static String VOID_TEARS_SELECTED_POINT = "";
    public static int CLIENT_TRAGIC_HEART = 0;
    public static boolean CLIENT_VOID_TEARS_PERMISSION = false;
    public static boolean PURE_FIRE_PERMISSION = false;
    public static boolean PURE_FIRE_MEDITATING = false;
    public static boolean PURE_FIRE_CASTING = false;
    public static boolean PURE_FIRE_RELEASING = false;
    public static boolean PURE_FIRE_HAND_FORM = false;
    public static int PURE_FIRE_PROFICIENCY = 0;
    public static int PURE_FIRE_SELECTED_SLOT = 0;
    public static int PURE_FIRE_SELECTED_SCHEME = 0;
    public static boolean PURE_YANG_BODY_PROTECTION = false;

    public static int PURE_FIRE_MEDITATION_PROGRESS = 0;
    public static int PURE_FIRE_TALENT_POINTS = 0;
    public static int PURE_FIRE_MEDITATION_LAYERS = 0;
    public static long PURE_FIRE_MEDITATION_CD_REMAINING = 0;
    public static int PURE_FIRE_UNLOCKED_MASK = 0;
    public static int[] PURE_FIRE_SLOT_SKILLS = new int[] {1, 2, 3, 4, 5, 6, 7, 8};
    public static float PURE_FIRE_CHARGE_PROGRESS = 0.0F;
    private static boolean pureFireCastSent = false;
    private static boolean pureYangFormSelecting = false;
    private static boolean pureYangShiftWasDown = false;
    private static int pureFireReleaseAnimationTicks = 0;

    /** Register a Player Animator stack for the local player. The stack is
     * intentionally empty for now; future keyframe clips can be layered here
     * without changing the input or server-authoritative skill code. */
    static {
        try {
            dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess.REGISTER_ANIMATION_EVENT
                    .register((player, stack) -> stack.addAnimLayer(100,
                    new PureFireHandAnimation(player)));
        } catch (NoClassDefFoundError ignored) {
            // Development environments may compile without the optional client
            // classes; the Forge dependency remains mandatory at runtime.
        }
    }

    /** Player Animator layer for charging and the palm-blast gesture. Keeping this in its own
     * layer means future emotes or animation packs can coexist without being
     * overwritten by the pure-fire skill. */
    private static final class PureFireHandAnimation implements IAnimation {
        private final AbstractClientPlayer player;

        private PureFireHandAnimation(AbstractClientPlayer player) {
            this.player = player;
        }

        @Override public boolean isActive() {
            // Skill 2 is a third-person-only attached model. It must not
            // activate Player Animator's first-person full-player pass.
            if (player == Minecraft.getInstance().player) {
                return PURE_FIRE_CASTING || findTrackedFlame() != null
                        || findTrackedPunch() != null || findTrackedMeteorDive() != null
                        || findTrackedCleave() != null || findTrackedThrust() != null
                        || findTrackedWallCast() != null || findTrackedBallCast() != null;
            }
            return findTrackedFlame() != null
                    || findTrackedPunch() != null || findTrackedMeteorDive() != null
                    || findTrackedCleave() != null || findTrackedThrust() != null
                    || findTrackedWallCast() != null || findTrackedBallCast() != null;
        }

        @Override public Vec3f get3DTransform(String modelName, TransformType type,
                                               float tickDelta, Vec3f value0) {
            boolean arm = modelName.equals("leftArm") || modelName.equals("rightArm");
            boolean punchPosePart = modelName.equals("head") || modelName.equals("torso")
                    || modelName.equals("leftLeg") || modelName.equals("rightLeg");
            if (!isActive() || !(arm || punchPosePart)) {
                return value0;
            }
            PureFireVortexEntity tracked = findTrackedFlame();
            PureFirePunchEntity punch = findTrackedPunch();
            PureFireMeteorDiveEntity meteorDive = findTrackedMeteorDive();
            PureFireCleaveEntity cleave = findTrackedCleave();
            PureFireThrustEntity thrust = findTrackedThrust();
            com.mjmod.entity.PureFireWallCastEntity wallCast = findTrackedWallCast();
            com.mjmod.entity.PureFireBallCastEntity ballCast = findTrackedBallCast();
            boolean handFormOnly = punch == null && meteorDive == null && tracked == null
                    && cleave == null && thrust == null && wallCast == null && ballCast == null
                    && (player == Minecraft.getInstance().player
                    ? PURE_FIRE_HAND_FORM : player.hasEffect(ModEffects.PURE_FIRE_HAND_FORM.get()));
            if (handFormOnly) return value0;
            if (ballCast != null) {
                if (type == TransformType.ROTATION) {
                    return ballCastRotation(ballCast, modelName, tickDelta, value0);
                }
                return value0;
            }
            if (wallCast != null) {
                if (type == TransformType.ROTATION) {
                    return wallCastRotation(wallCast, modelName, tickDelta, value0);
                }
                return value0;
            }
            if (meteorDive != null) {
                if (type == TransformType.POSITION) {
                    return meteorDivePosition(meteorDive, modelName, tickDelta, value0);
                }
                if (type == TransformType.ROTATION) {
                    return meteorDiveRotation(meteorDive, modelName, tickDelta, value0);
                }
                return value0;
            }
            if (cleave != null) {
                if (type == TransformType.ROTATION) {
                    return cleaveRotation(cleave, modelName, tickDelta, value0);
                }
                return value0;
            }
            if (thrust != null) {
                if (type == TransformType.ROTATION) {
                    return thrustRotation(thrust, modelName, tickDelta, value0);
                }
                return value0;
            }
            if (type != TransformType.ROTATION) return value0;
            if (punch != null && punchPosePart) {
                float progress = punch.getModelAnimationProgress(tickDelta);
                float blend = punchPoseBlend(progress);
                if (modelName.equals("head")) {
                    Vec3 direction = punch.getPunchDirection();
                    float targetYaw = (float) Math.toDegrees(
                            Math.atan2(-direction.x, direction.z));
                    float relativeYaw = Mth.wrapDegrees(targetYaw - player.getYRot())
                            * Mth.DEG_TO_RAD;
                    float targetPitch = (float) -Math.asin(Mth.clamp(direction.y, -1.0D, 1.0D));
                    // Keep the head tracking the punch direction throughout
                    // the strike, while vanilla body yaw remains unchanged.
                    return new Vec3f(targetPitch * blend, relativeYaw * blend, 0.0F);
                }
                if (modelName.equals("torso")) {
                    return new Vec3f(-0.16F * blend, 0.0F, 0.0F);
                }
                // Legs are separate Player Animator bones, so carry the torso's
                // live strike pitch into both leg bones. Keep a small authored
                // leg-specific base pose so the leg straps move with the body
                // without becoming a rigid copy of the torso.
                float torsoPitch = -0.16F * blend;
                float legBase = modelName.equals("rightLeg") ? 0.06F : 0.04F;
                return new Vec3f(torsoPitch + legBase * blend, 0.0F, 0.0F);
            }
            if (!arm) return value0;
            boolean releasing = player == Minecraft.getInstance().player
                    ? PURE_FIRE_RELEASING : tracked != null && tracked.isReleased();
            boolean left = modelName.equals("leftArm");
            if (punch != null) {
                // Uploaded 直冲 keyframes: arms rise into the final punch
                // pose over the 0.54s clip, hold it during the dash, then
                // reverse back during the six-tick retract phase.
                float progress = punch.getModelAnimationProgress(tickDelta);
                float rightRotation = Mth.lerp(smooth(progress),
                        -2.5F * Mth.DEG_TO_RAD, -87.5F * Mth.DEG_TO_RAD);
                // The uploaded pose is asymmetric: right hand punches while
                // the left hand retracts to the waist/hip.
                float leftRotation = Mth.lerp(smooth(progress),
                        0.0F, 38.0F * Mth.DEG_TO_RAD);
                if (left) return new Vec3f(leftRotation, 0.18F, -0.22F);
                return new Vec3f(rightRotation, -0.04F, 0.02F);
            }

            if (!releasing) {
                float charge = player == Minecraft.getInstance().player
                        ? PURE_FIRE_CHARGE_PROGRESS
                        : (tracked == null ? 0.0F : tracked.getCharge());
                float sway = Mth.sin((player.tickCount + tickDelta) * 0.22F)
                        * (0.018F + charge * 0.018F);
                // Charging: both hands hang beside the body.
                return new Vec3f(0.04F + sway, left ? -0.04F : 0.04F,
                        left ? -0.035F : 0.035F);
            }

            // Release: Iron-Man-style right-palm blast. The right arm extends
            // toward the current crosshair while the left arm remains lowered.
            float blend = player == Minecraft.getInstance().player
                    ? Mth.clamp((pureFireReleaseAnimationTicks + tickDelta) / 8.0F,
                    0.0F, 1.0F) : 1.0F;
            blend = blend * blend * (3.0F - 2.0F * blend);
            if (left) return new Vec3f(0.12F * blend, -0.05F * blend, -0.08F * blend);
            float crosshairPitch = player.getXRot() * Mth.DEG_TO_RAD;
            float bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
            float crosshairYaw = Mth.wrapDegrees(player.getYRot() - bodyYaw)
                    * Mth.DEG_TO_RAD;
            return new Vec3f((-1.52F + crosshairPitch) * blend,
                    (-0.08F + crosshairYaw) * blend, 0.03F * blend);
        }

        @Override public void setupAnim(float tickDelta) { }

        private static float smooth(float value) {
            float t = Mth.clamp(value, 0.0F, 1.0F);
            return t * t * (3.0F - 2.0F * t);
        }

        private static float punchPoseBlend(float progress) {
            return smooth(progress);
        }

        @Override public FirstPersonMode getFirstPersonMode(float tickDelta) {
            if (!isActive()) return FirstPersonMode.NONE;
            PureFireVortexEntity flame = findTrackedFlame();
            boolean firstSkillActive = player == Minecraft.getInstance().player
                    ? PURE_FIRE_CASTING || flame != null : flame != null;
            // Skills 2-4 now show the gauntlet model in first person too:
            // hand form, an active 直冲拳 or a 增压 meteor dive all switch the
            // player-anim pass to the third-person model, where
            // PureFireHandsLayer draws the new gauntlet on both arms.
            boolean localPlayer = player == Minecraft.getInstance().player;
            boolean gauntletSkillActive = localPlayer
                    ? PURE_FIRE_HAND_FORM || findTrackedPunch() != null
                            || findTrackedMeteorDive() != null
                    : player.hasEffect(ModEffects.PURE_FIRE_HAND_FORM.get())
                            || findTrackedPunch() != null || findTrackedMeteorDive() != null;
            return firstSkillActive || gauntletSkillActive
                    ? FirstPersonMode.THIRD_PERSON_MODEL : FirstPersonMode.NONE;
        }

        @Override public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
            return new FirstPersonConfiguration()
                    .setShowLeftArm(true)
                    .setShowRightArm(true)
                    .setShowLeftItem(true)
                    .setShowRightItem(true);
        }

        private PureFireVortexEntity findTrackedFlame() {
            if (player.level() == null) return null;
            List<PureFireVortexEntity> flames = player.level().getEntitiesOfClass(
                    PureFireVortexEntity.class, player.getBoundingBox().inflate(32.0D),
                    flame -> flame.getOwner() == player && !flame.isRemoved());
            return flames.isEmpty() ? null : flames.get(0);
        }

        private PureFirePunchEntity findTrackedPunch() {
            if (player.level() == null) return null;
            List<PureFirePunchEntity> punches = player.level().getEntitiesOfClass(
                    PureFirePunchEntity.class, player.getBoundingBox().inflate(3.0D),
                    punch -> punch.getOwner() == player && !punch.isRemoved());
            return punches.isEmpty() ? null : punches.get(0);
        }

        private PureFireMeteorDiveEntity findTrackedMeteorDive() {
            if (player.level() == null) return null;
            List<PureFireMeteorDiveEntity> dives = player.level().getEntitiesOfClass(
                    PureFireMeteorDiveEntity.class, player.getBoundingBox().inflate(48.0D),
                    dive -> dive.getOwner() == player && !dive.isRemoved());
            return dives.isEmpty() ? null : dives.get(0);
        }

        private PureFireCleaveEntity findTrackedCleave() {
            if (player.level() == null) return null;
            List<PureFireCleaveEntity> cleaves = player.level().getEntitiesOfClass(
                    PureFireCleaveEntity.class, player.getBoundingBox().inflate(16.0D),
                    cleave -> cleave.getOwner() == player && !cleave.isRemoved());
            return cleaves.isEmpty() ? null : cleaves.get(0);
        }

        private PureFireThrustEntity findTrackedThrust() {
            if (player.level() == null) return null;
            List<PureFireThrustEntity> thrusts = player.level().getEntitiesOfClass(
                    PureFireThrustEntity.class, player.getBoundingBox().inflate(16.0D),
                    thrust -> thrust.getOwner() == player && !thrust.isRemoved());
            return thrusts.isEmpty() ? null : thrusts.get(0);
        }

        private com.mjmod.entity.PureFireWallCastEntity findTrackedWallCast() {
            if (player.level() == null) return null;
            List<com.mjmod.entity.PureFireWallCastEntity> casts = player.level().getEntitiesOfClass(
                    com.mjmod.entity.PureFireWallCastEntity.class, player.getBoundingBox().inflate(16.0D),
                    cast -> cast.getOwner() == player && !cast.isRemoved());
            return casts.isEmpty() ? null : casts.get(0);
        }

        private com.mjmod.entity.PureFireBallCastEntity findTrackedBallCast() {
            if (player.level() == null) return null;
            List<com.mjmod.entity.PureFireBallCastEntity> casts = player.level().getEntitiesOfClass(
                    com.mjmod.entity.PureFireBallCastEntity.class, player.getBoundingBox().inflate(16.0D),
                    cast -> cast.getOwner() == player && !cast.isRemoved());
            return casts.isEmpty() ? null : casts.get(0);
        }

        /**
         * 纯质阳炎·烈阳火球施法动作 (Player Animator 骨骼插值)：
         * 1. 蓄力阶段 (Charging)：
         *    - Shift 蓄力召唤 8 球护体：双手自然向左右斜下方微展（如参考图2中东方灵族仙气飘逸的御火站姿），身体挺拔，掌心朝天。
         *    - 普通蓄力直掷：右手单手高高托举（斜上方向天），左手自然展开平衡。
         * 2. 下砸释放阶段 (Release)：
         *    - 右手雷霆之势自高处向准星猛烈劈击下砸，身体前倾带动冲势。
         */
        private static Vec3f ballCastRotation(com.mjmod.entity.PureFireBallCastEntity cast,
                                              String modelName, float tickDelta, Vec3f fallback) {
            boolean released = cast.isReleased();
            float releaseProg = cast.getReleaseProgress(tickDelta);
            // Shift 环绕火球已移除，九技能始终走单手托举下砸。
            if (modelName.equals("rightArm")) {
                if (!released) {
                    // 蓄力微震晃动
                    float sway = Mth.sin((cast.tickCount + tickDelta) * 0.3F) * 0.035F;
                    // 单手托天聚火：右臂向斜上方充分抬起（-105°），微微外展，气势雄浑
                    return new Vec3f(-1.85F + sway, -0.22F, -0.28F);
                } else {
                    // 0.0s -> 0.4s (8 ticks): 猛烈下劈前砸 (从 -105° 强力向前下方下砸到 +20°)
                    float t = smooth(releaseProg);
                    float xRot = Mth.lerp(t, -1.85F, 0.35F);
                    float zRot = Mth.lerp(t, -0.28F, 0.05F);
                    return new Vec3f(xRot, -0.12F, zRot);
                }
            }
            if (modelName.equals("leftArm")) {
                if (!released) {
                    // 左手自然向后外展以平衡身体
                    return new Vec3f(0.20F, 0.12F, 0.30F);
                } else {
                    float t = smooth(releaseProg);
                    return new Vec3f(Mth.lerp(t, 0.20F, 0.50F), 0.12F, 0.40F);
                }
            }
            if (modelName.equals("torso")) {
                if (!released) {
                    // 身体重心微微后引蓄势
                    return new Vec3f(-0.10F, 0.06F, 0.0F);
                } else {
                    // 下砸时上半身顺势前倾
                    float t = smooth(releaseProg);
                    return new Vec3f(Mth.lerp(t, -0.10F, 0.20F), 0.04F, 0.0F);
                }
            }
            if (modelName.equals("head")) {
                // 头部稳定盯紧目标瞄准方向
                return new Vec3f(0.0F, 0.0F, 0.0F);
            }
            return fallback;
        }

        /**
         * 基础推掌/拍地动作预留（供用户制作 Blockbench 动作替换）。
         * 时长 1.29s (26 ticks)，0.4s (8 ticks / progress ~0.31) 释火推掌。
         */
        private static Vec3f wallCastRotation(com.mjmod.entity.PureFireWallCastEntity cast,
                                              String modelName, float tickDelta, Vec3f fallback) {
            float progress = cast.getAnimationProgress(tickDelta);
            // 0.0s -> 0.4s (0.0 -> 0.31): 双手聚气蓄力
            // 0.4s -> 0.8s (0.31 -> 0.62): 双手猛烈下拍或前推
            // 0.8s -> 1.29s (0.62 -> 1.0): 保持并从容收势回正
            if (modelName.equals("rightArm") || modelName.equals("leftArm")) {
                if (progress <= 0.31F) {
                    float t = smooth(progress / 0.31F);
                    return new Vec3f(Mth.lerp(t, 0.0F, -45.0F * Mth.DEG_TO_RAD),
                            0.0F,
                            modelName.equals("rightArm") ? Mth.lerp(t, 0.0F, -25.0F * Mth.DEG_TO_RAD)
                                    : Mth.lerp(t, 0.0F, 25.0F * Mth.DEG_TO_RAD));
                } else if (progress <= 0.62F) {
                    float t = smooth((progress - 0.31F) / 0.31F);
                    return new Vec3f(Mth.lerp(t, -45.0F * Mth.DEG_TO_RAD, -85.0F * Mth.DEG_TO_RAD),
                            0.0F, 0.0F);
                } else {
                    float t = smooth((progress - 0.62F) / 0.38F);
                    return new Vec3f(Mth.lerp(t, -85.0F * Mth.DEG_TO_RAD, 0.0F), 0.0F, 0.0F);
                }
            }
            if (modelName.equals("torso")) {
                if (progress <= 0.31F) {
                    float t = smooth(progress / 0.31F);
                    return new Vec3f(Mth.lerp(t, 0.0F, 10.0F * Mth.DEG_TO_RAD), 0.0F, 0.0F);
                } else if (progress <= 0.62F) {
                    float t = smooth((progress - 0.31F) / 0.31F);
                    return new Vec3f(Mth.lerp(t, 10.0F * Mth.DEG_TO_RAD, 20.0F * Mth.DEG_TO_RAD), 0.0F, 0.0F);
                } else {
                    float t = smooth((progress - 0.62F) / 0.38F);
                    return new Vec3f(Mth.lerp(t, 20.0F * Mth.DEG_TO_RAD, 0.0F), 0.0F, 0.0F);
                }
            }
            return fallback;
        }

        /**
         * Keyframe interpolation for “横斩as老冯” (length 1.0s / 24 frames).
         * Right Arm keyframes:
         * 0s: rot [0, 0, 0]
         * 0.41667s: rot [90, 60, 0]
         * 0.83333s: rot [0, -90, 90]
         */
        private static Vec3f cleaveRotation(PureFireCleaveEntity cleave, String modelName,
                                            float tickDelta, Vec3f fallback) {
            float progress = cleave.getAnimationProgress(tickDelta);
            if (modelName.equals("rightArm")) {
                if (progress <= 0.41667F) {
                    float t = smooth(progress / 0.41667F);
                    return new Vec3f(Mth.lerp(t, 0.0F, -90.0F * Mth.DEG_TO_RAD),
                            Mth.lerp(t, 0.0F, 60.0F * Mth.DEG_TO_RAD), 0.0F);
                } else if (progress <= 0.83333F) {
                    float t = smooth((progress - 0.41667F) / 0.41666F);
                    return new Vec3f(Mth.lerp(t, -90.0F * Mth.DEG_TO_RAD, 0.0F),
                            Mth.lerp(t, 60.0F * Mth.DEG_TO_RAD, -90.0F * Mth.DEG_TO_RAD),
                            Mth.lerp(t, 0.0F, 90.0F * Mth.DEG_TO_RAD));
                } else {
                    float t = smooth((progress - 0.83333F) / 0.16667F);
                    return new Vec3f(0.0F,
                            Mth.lerp(t, -90.0F * Mth.DEG_TO_RAD, 0.0F),
                            Mth.lerp(t, 90.0F * Mth.DEG_TO_RAD, 0.0F));
                }
            }
            if (modelName.equals("torso") || modelName.equals("head")) {
                float twist = Mth.sin(progress * Mth.PI) * (modelName.equals("torso") ? 0.35F : -0.2F);
                return new Vec3f(0.0F, twist, 0.0F);
            }
            return fallback;
        }

        /**
         * Keyframe interpolation for “刺杀as老冯” (length 1.125s / 27 frames).
         * Right Arm:
         * 0s: [0, 0, 0]
         * 0.375s: [90, 0, 0]
         * 0.70833s: [90, -7.5, 0]
         * 1.125s: [0, 0, 0]
         */
        private static Vec3f thrustRotation(PureFireThrustEntity thrust, String modelName,
                                            float tickDelta, Vec3f fallback) {
            float progress = thrust.getAnimationProgress(tickDelta);
            if (modelName.equals("rightArm")) {
                if (progress <= 0.375F) {
                    float t = smooth(progress / 0.375F);
                    return new Vec3f(Mth.lerp(t, 0.0F, -90.0F * Mth.DEG_TO_RAD), 0.0F, 0.0F);
                } else if (progress <= 0.70833F) {
                    float t = smooth((progress - 0.375F) / 0.33333F);
                    return new Vec3f(-90.0F * Mth.DEG_TO_RAD,
                            Mth.lerp(t, 0.0F, -7.5F * Mth.DEG_TO_RAD), 0.0F);
                } else {
                    float t = smooth((progress - 0.70833F) / 0.29167F);
                    return new Vec3f(Mth.lerp(t, -90.0F * Mth.DEG_TO_RAD, 0.0F),
                            Mth.lerp(t, -7.5F * Mth.DEG_TO_RAD, 0.0F), 0.0F);
                }
            }
            if (modelName.equals("torso")) {
                if (progress <= 0.375F) {
                    float t = smooth(progress / 0.375F);
                    return new Vec3f(Mth.lerp(t, 0.0F, 0.06F),
                            Mth.lerp(t, 0.0F, -0.78F), 0.0F);
                } else {
                    float t = smooth((progress - 0.375F) / 0.625F);
                    return new Vec3f(Mth.lerp(t, 0.06F, 0.0F),
                            Mth.lerp(t, -0.78F, 0.0F), 0.0F);
                }
            }
            return fallback;
        }

        private static Vec3f meteorDiveRotation(PureFireMeteorDiveEntity dive,
                                                String modelName, float tickDelta,
                                                Vec3f fallback) {
            float time = dive.getAnimationTimeSeconds(tickDelta);
            float xDegrees;
            if (time <= 1.0F) {
                float t = smooth(time);
                xDegrees = switch (modelName) {
                    case "head" -> Mth.lerp(t, 0.0F, -22.5F);
                    case "torso" -> 0.0F;
                    case "rightArm" -> Mth.lerp(t, 2.5F, -149.69F);
                    case "leftArm" -> Mth.lerp(t, 2.5F, -152.5F);
                    case "rightLeg" -> Mth.lerp(t, 0.0F, 12.5F);
                    case "leftLeg" -> 0.0F;
                    default -> 0.0F;
                };
            } else {
                float t = smooth((time - 1.0F) / 0.79167F);
                xDegrees = switch (modelName) {
                    case "head" -> Mth.lerp(t, -22.5F, 37.5F);
                    case "torso" -> Mth.lerp(t, 0.0F, 92.5F);
                    case "rightArm" -> Mth.lerp(t, -149.69F, -37.5F);
                    case "leftArm" -> Mth.lerp(t, -152.5F, -25.0F);
                    case "rightLeg" -> Mth.lerp(t, 12.5F, 100.0851F);
                    case "leftLeg" -> Mth.lerp(t, 0.0F, 97.535F);
                    default -> 0.0F;
                };
            }
            return new Vec3f(xDegrees * Mth.DEG_TO_RAD,
                    fallback.getY(), fallback.getZ());
        }

        private static Vec3f meteorDivePosition(PureFireMeteorDiveEntity dive,
                                                String modelName, float tickDelta,
                                                Vec3f value0) {
            float time = dive.getAnimationTimeSeconds(tickDelta);
            if (time <= 1.0F) return value0;

            // Latest uploaded 爆冲 clip uses linear position tracks from
            // 1.0s to 1.79167s. Player Animator expects ModelPart pixels and
            // absolute pivots, so preserve value0 and add the Blockbench
            // offset. Its non-body coordinate conversion is X, -Y, Z.
            float t = Mth.clamp((time - 1.0F) / 0.79167F, 0.0F, 1.0F);
            return switch (modelName) {
                case "rightLeg" -> new Vec3f(value0.getX(),
                        value0.getY() - 13.0F * t,
                        value0.getZ() + 11.0F * t);
                case "leftLeg" -> new Vec3f(value0.getX(),
                        value0.getY() - 13.0F * t,
                        value0.getZ() + 12.0F * t);
                case "head" -> new Vec3f(value0.getX(), value0.getY(),
                        value0.getZ() - 1.0F * t);
                default -> value0;
            };
        }
    }

    public static void setVoidTearsActive(boolean active) {
        VOID_TEARS_ACTIVE = active;
    }

    public static void setVoidTearsPoints(
            List<VoidTearsSkillManager.CoordinatePoint> points, String selectedUid) {
        VOID_TEARS_POINTS = Collections.unmodifiableList(new ArrayList<>(points));
        VOID_TEARS_SELECTED_POINT = selectedUid == null ? "" : selectedUid;
    }

    /** Synchronizes the two client-visible Void Tears values. */
    public static void setVoidTearsData(int tragicHeart, boolean permission) {
        CLIENT_TRAGIC_HEART = Math.max(0, tragicHeart);
        CLIENT_VOID_TEARS_PERMISSION = permission;
    }

    public static void setPureFireStatus(boolean permission, boolean meditating,
                                         boolean casting, boolean releasing, boolean handForm,
                                         int proficiency, int selectedSlot, int selectedScheme,
                                         boolean bodyProtection, int meditationProgress,
                                         int talentPoints, int meditationLayers,
                                         long meditationCd, int unlockedMask, int[] slotSkills) {
        PURE_FIRE_PERMISSION = permission;
        PURE_FIRE_MEDITATING = meditating;
        if (!casting) PURE_FIRE_CHARGE_PROGRESS = 0.0F;
        PURE_FIRE_CASTING = casting;
        PURE_FIRE_RELEASING = releasing;
        PURE_FIRE_HAND_FORM = handForm;
        PURE_FIRE_PROFICIENCY = Math.max(0, proficiency);
        PURE_FIRE_SELECTED_SLOT = Math.max(0, Math.min(7, selectedSlot));
        PURE_FIRE_SELECTED_SCHEME = Math.max(0, Math.min(2, selectedScheme));
        PURE_YANG_BODY_PROTECTION = bodyProtection;
        PURE_FIRE_MEDITATION_PROGRESS = Math.max(0, Math.min(100, meditationProgress));
        PURE_FIRE_TALENT_POINTS = Math.max(0, talentPoints);
        PURE_FIRE_MEDITATION_LAYERS = Math.max(0, meditationLayers);
        PURE_FIRE_MEDITATION_CD_REMAINING = Math.max(0L, meditationCd);
        PURE_FIRE_UNLOCKED_MASK = unlockedMask;
        if (slotSkills != null && slotSkills.length == 8) {
            PURE_FIRE_SLOT_SKILLS = slotSkills.clone();
        }
    }

    public static int SKILL_ANIMATION_TIMER = 0;
    public static int SKILL_ANIMATION_DURATION = 0;
    public static int SKILL_ANIMATION_TYPE = 0;
    private static final java.util.Map<java.util.UUID, Boolean> THUNDER_ACCEL_ACTIVE =
            new java.util.concurrent.ConcurrentHashMap<>();
    public static int THUNDER_ACCEL_REMAINING = 0;
    public static int THUNDER_ACCEL_COOLDOWN = 0;
    private static int THUNDER_ACCEL_WAVE_TICKS = 0;
    private static float THUNDER_ACCEL_WAVE_RADIUS = 0.0F;

    public static void setThunderAccel(java.util.UUID playerId, boolean active, int remaining, int cooldown) {
        boolean wasActive = Boolean.TRUE.equals(THUNDER_ACCEL_ACTIVE.get(playerId));
        if (active) THUNDER_ACCEL_ACTIVE.put(playerId, Boolean.TRUE);
        else THUNDER_ACCEL_ACTIVE.remove(playerId);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(playerId)) {
            THUNDER_ACCEL_REMAINING = remaining;
            THUNDER_ACCEL_COOLDOWN = cooldown;
            if (active && !wasActive) {
                THUNDER_ACCEL_WAVE_TICKS = 24;
                THUNDER_ACCEL_WAVE_RADIUS = 0.0F;
            }
        }
    }

    public static boolean isThunderAccelActive(java.util.UUID playerId) {
        return Boolean.TRUE.equals(THUNDER_ACCEL_ACTIVE.get(playerId));
    }

    public static void showSkillAnimation(int skillId, int durationTicks) {
        SKILL_ANIMATION_TYPE = skillId;
        SKILL_ANIMATION_DURATION = Math.max(1, durationTicks);
        SKILL_ANIMATION_TIMER = SKILL_ANIMATION_DURATION;
    }

    private static final ResourceLocation CAST_TEXTURE =
            new ResourceLocation(MJMod.MODID, "textures/entity/void_tear_beam.png");
    private static VoidTearCastAnimationModel castModel;
    private static ModelPart castPart;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ThunderAccelAfterimage.renderWorld(event);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null || SKILL_ANIMATION_TIMER <= 0) return;

        if (castPart == null) {
            castPart = mc.getEntityModels().bakeLayer(VoidTearCastAnimationModel.LAYER_LOCATION);
            castModel = new VoidTearCastAnimationModel(castPart);
        }
        float partialTick = event.getPartialTick();
        float time = mc.level.getGameTime() + partialTick;
        float elapsed = SKILL_ANIMATION_DURATION - SKILL_ANIMATION_TIMER + partialTick;
        float progress = Mth.clamp(elapsed / (float) SKILL_ANIMATION_DURATION, 0.0F, 1.0F);
        float fade = Mth.sin(progress * Mth.PI);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 0.0F);
        float scale = 0.12F + Mth.sin(progress * Mth.PI) * 0.12F;
        poseStack.scale(scale, scale, scale);
        castModel.animate(time, progress);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(CAST_TEXTURE));
        castModel.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY,
                0.55F, 0.86F, 1.0F, 0.74F * fade);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!PURE_FIRE_PERMISSION) return;

        // 按住 Ctrl 时，用滚轮上下切换纯质阳炎 8 个技能槽
        if (Screen.hasControlDown()) {
            double scroll = event.getScrollDelta();
            if (scroll != 0) {
                boolean reverse = scroll > 0; // 向上滚向后倒退，向下滚向前前进
                PURE_FIRE_SELECTED_SLOT = Math.floorMod(PURE_FIRE_SELECTED_SLOT + (reverse ? -1 : 1), 8);
                MJNetwork.sendPureFireSkill(0, PURE_FIRE_SELECTED_SLOT, false);
                event.setCanceled(true); // 拦截原版切换物品栏快捷栏
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        spawnPureFireAuraParticles(mc);
        tickThunderAccelWave(mc);

        boolean shiftDown = mc.options.keyShift != null && mc.options.keyShift.isDown();
        while (ModKeyBindings.PURE_FIRE_SWITCH.consumeClick()) {
            // G only changes A/B/D while the fifth skill's Shift-held form
            // selector is open. Outside that transaction it always cycles the
            // five normal skill slots.
            MJNetwork.sendPureFireSkill(pureYangFormSelecting ? 5 : 0, -1, false);
        }
        boolean ctrlRightClickDown = Screen.hasControlDown() && mc.options.keyUse != null && mc.options.keyUse.isDown();
        boolean pureFireDown = ModKeyBindings.PURE_FIRE_CAST.isDown() || ctrlRightClickDown;
        int activeSkillId = (PURE_FIRE_SELECTED_SLOT < PURE_FIRE_SLOT_SKILLS.length)
                ? PURE_FIRE_SLOT_SKILLS[PURE_FIRE_SELECTED_SLOT] : (PURE_FIRE_SELECTED_SLOT + 1);
        boolean holdSkill = activeSkillId == 1 || activeSkillId == PureFireSkillManager.SKILL_FIRE_BALL;
        if (pureFireDown && !pureFireCastSent) {
            if (activeSkillId == 5 && shiftDown) {
                pureYangFormSelecting = true;
                MJNetwork.sendPureFireSkill(6, 0, false);
            } else {
                MJNetwork.sendPureFireSkill(1, 0, shiftDown);
            }
            pureFireCastSent = true;
        } else if (!pureFireDown && pureFireCastSent && holdSkill) {
            MJNetwork.sendPureFireSkill(2, 0, false);
            pureFireCastSent = false;
        } else if (!pureFireDown && pureFireCastSent) {
            pureFireCastSent = false;
        }
        if (pureYangFormSelecting && pureYangShiftWasDown && !shiftDown) {
            // Releasing Shift, not releasing C, commits the selected form.
            MJNetwork.sendPureFireSkill(7, 0, false);
            pureYangFormSelecting = false;
        }
        pureYangShiftWasDown = shiftDown;
        if (PURE_FIRE_CASTING) {
            // Server charge duration is 60 ticks (three one-second phases).
            // This local interpolation keeps the HUD responsive between
            // status packets while the server remains authoritative.
            PURE_FIRE_CHARGE_PROGRESS = Math.min(1.0F,
                    PURE_FIRE_CHARGE_PROGRESS + 1.0F / PureFireSkillManager.CHARGE_TOTAL_TICKS);
        }
        if (PURE_FIRE_RELEASING) {
            pureFireReleaseAnimationTicks = Math.min(8, pureFireReleaseAnimationTicks + 1);
        } else {
            pureFireReleaseAnimationTicks = 0;
        }
        while (ModKeyBindings.PURE_FIRE_MEDITATE.consumeClick()) {
            MJNetwork.sendPureFireSkill(3, 0, false);
        }
        while (ModKeyBindings.OPEN_PURE_FIRE_TALENT.consumeClick()) {
            if (!PURE_FIRE_PERMISSION || (PURE_FIRE_UNLOCKED_MASK & 1) == 0) {
                mc.player.displayClientMessage(Component.literal("§c你尚未领悟纯质阳炎基础（一技能），无法开启天赋树。"), true);
            } else if (mc.screen == null) {
                mc.setScreen(new PureFireTalentTreeScreen());
            }
        }

        while (ModKeyBindings.VOID_TEARS_SWITCH.consumeClick()) {
            boolean reverse = mc.options.keyShift != null && mc.options.keyShift.isDown();
            VOID_TEARS_SELECTED_SKILL = reverse
                    ? switch (VOID_TEARS_SELECTED_SKILL) {
                        case 1 -> 5;
                        case 2 -> 1;
                        case 3 -> 2;
                        case 4 -> 3;
                        default -> 4;
                    }
                    : switch (VOID_TEARS_SELECTED_SKILL) {
                        case 1 -> 2;
                        case 2 -> 3;
                        case 3 -> 4;
                        case 4 -> 5;
                        default -> 1;
                    };
            MJNetwork.sendVoidTearsSkill(0, VOID_TEARS_SELECTED_SKILL);
        }
        while (ModKeyBindings.VOID_TEARS_CAST.consumeClick()) {
            boolean shift = mc.options.keyShift != null && mc.options.keyShift.isDown();
            if (shift && VOID_TEARS_SELECTED_SKILL == 3) {
                if (mc.screen == null) mc.setScreen(new VoidTearsTeleportGui(true));
                continue;
            }
            MJNetwork.sendVoidTearsSkill(1, VOID_TEARS_SELECTED_SKILL, shift);
        }
        while (ModKeyBindings.BLOOD_EXCHANGE.consumeClick()) {
            MJNetwork.sendBloodExchange();
        }
        while (ModKeyBindings.OPEN_VOID_TEARS_POINTS.consumeClick()) {
            if (!CLIENT_VOID_TEARS_PERMISSION) {
                mc.player.displayClientMessage(Component.literal(
                        "§c需要先使用虚空之泪消耗品获得法宝权限。"), true);
            } else if (mc.screen == null) {
                mc.setScreen(new VoidTearsTeleportGui(false));
            }
        }
        while (ModKeyBindings.OPEN_VOID_TEARS_HEART.consumeClick()) {
            if (!CLIENT_VOID_TEARS_PERMISSION) {
                mc.player.displayClientMessage(Component.literal(
                        "§c需要先使用虚空之泪消耗品，才能查看悲情之心。"), true);
            } else if (mc.screen == null) {
                mc.setScreen(new VoidTearsHeartGui());
            }
            MJNetwork.requestVoidTearsData();
        }
        while (ModKeyBindings.THUNDER_ACCEL.consumeClick()) {
            MJNetwork.sendThunderAccelToggle();
        }

        if (mc.level != null) {
            for (Player tracked : mc.level.players()) {
                ThunderAccelAfterimage.tick(tracked, isThunderAccelActive(tracked.getUUID()));
            }
        }

        if (SKILL_ANIMATION_TIMER > 0) SKILL_ANIMATION_TIMER--;
    }

    private static int pureFireScheme(Player player) {
        if (player.hasEffect(ModEffects.PURE_FIRE_SCHEME_A.get())) return PureFireSkillManager.SCHEME_A;
        if (player.hasEffect(ModEffects.PURE_FIRE_SCHEME_B.get())) return PureFireSkillManager.SCHEME_B;
        if (player.hasEffect(ModEffects.PURE_FIRE_SCHEME_D.get())) return PureFireSkillManager.SCHEME_D;
        return -1;
    }

    /**
     * DragonBall-style two-layer power-up aura spawner.
     * Outer layer: large translucent flame tongues kept below eye height and
     * pushed away from the camera radius so the first-person view stays clear.
     * Inner layer: small white-hot core sparks hugging the body.
     * Both layers are also thinned out for the local player in first person.
     */
    private static void spawnPureFireAuraParticles(Minecraft mc) {
        if (mc.level == null) return;
        long gameTime = mc.level.getGameTime();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (Player player : mc.level.players()) {
            int scheme = pureFireScheme(player);
            if (scheme < 0) continue;

            boolean self = player == mc.player;
            float height = player.getBbHeight();
            double eyeY = player.getEyeY();

            // All three forms share the former defensive flame-shell model.
            // Their presentation remains distinct through density, radius,
            // orbit phase and the scheme payload decoded by the particles.
            int baseOuterCount = switch (scheme) {
                case PureFireSkillManager.SCHEME_B -> 9;
                case PureFireSkillManager.SCHEME_D -> 7;
                default -> 6;
            };
            int baseCoreCount = switch (scheme) {
                case PureFireSkillManager.SCHEME_B -> 5;
                case PureFireSkillManager.SCHEME_D -> 4;
                default -> 2;
            };
            double angularSpeed = switch (scheme) {
                case PureFireSkillManager.SCHEME_B -> 0.14D;
                case PureFireSkillManager.SCHEME_D -> 0.21D;
                default -> 0.31D;
            };
            double minRadius = switch (scheme) {
                case PureFireSkillManager.SCHEME_B -> 0.55D;
                case PureFireSkillManager.SCHEME_D -> 0.65D;
                default -> 0.78D;
            };
            double radiusSpread = switch (scheme) {
                case PureFireSkillManager.SCHEME_B -> 0.28D;
                case PureFireSkillManager.SCHEME_D -> 0.34D;
                default -> 0.36D;
            };

            // Outer flame-tongue layer (half rate in first person).
            int outerCount = firstPerson && self ? Math.max(2, baseOuterCount / 2) : baseOuterCount;
            for (int i = 0; i < outerCount; i++) {
                double angle = gameTime * angularSpeed + i * (Math.PI * 2.0D / outerCount)
                        + player.getRandom().nextDouble() * 0.38D;
                double radius = minRadius + player.getRandom().nextDouble() * radiusSpread;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + 0.05D
                        + player.getRandom().nextDouble() * Math.max(0.1D, eyeY - player.getY() - 0.35D);
                // The particle arguments are an attachment payload rather
                // than RGB: owner id, orbit angle, and scheme+orbit radius.
                mc.level.addParticle(ModParticles.PURE_FIRE_AURA.get(), x, y, z,
                        player.getId(), angle, scheme * 10.0D + radius);
            }

            // Inner white-hot core sparks.
            int coreCount = firstPerson && self ? Math.max(1, baseCoreCount / 2) : baseCoreCount;
            for (int i = 0; i < coreCount; i++) {
                double angle = gameTime * (angularSpeed + 0.07D) + i * (Math.PI * 2.0D / coreCount)
                        + player.getRandom().nextDouble() * 0.50D;
                double radius = scheme == PureFireSkillManager.SCHEME_B
                        ? 0.14D + player.getRandom().nextDouble() * 0.17D
                        : 0.18D + player.getRandom().nextDouble() * 0.22D;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                double y = player.getY() + 0.10D + player.getRandom().nextDouble() * height * 0.85D;
                mc.level.addParticle(ModParticles.PURE_FIRE_AURA_CORE.get(), x, y, z,
                        player.getId(), angle, scheme * 10.0D + radius);
            }
        }
    }


    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        if (mc.player == null) return;
        if (mc.player.hasEffect(ModEffects.DIZZINESS.get())) {
            graphics.fill(0, 0, mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(), 0xFF000000);
        }

        // 冥想打坐 HUD 状态展示（屏幕正下方）
        if (PURE_FIRE_MEDITATING) {
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();
            int cx = sw / 2;
            int by = sh - 75;
            int barW = 180;
            int barH = 8;
            int bx = cx - barW / 2;

            graphics.fill(bx - 2, by - 16, bx + barW + 2, by + barH + 14, 0x99100804);
            graphics.renderOutline(bx - 2, by - 16, barW + 4, barH + 30, 0xFFE09030);

            graphics.drawCenteredString(mc.font, "§6§l【纯质阳炎·凝神冥想中】", cx, by - 13, 0xFFFFFF);
            
            // 进度条底与充能
            graphics.fill(bx, by, bx + barW, by + barH, 0xDD20150A);
            int fill = (int) (barW * (Math.min(100, PURE_FIRE_MEDITATION_PROGRESS) / 100.0F));
            graphics.fill(bx, by, bx + fill, by + barH, 0xFFFFB020);
            graphics.renderOutline(bx, by, barW, barH, 0xFFFFAA00);

            String progressText = String.format("§e正在引火归元... %d%% (按 H 键退出)", PURE_FIRE_MEDITATION_PROGRESS);
            graphics.drawCenteredString(mc.font, progressText, cx, by + 10, 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public static void onSeveringMovement(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.hasEffect(ModEffects.SEVERING.get())) return;
        var input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onSeveringRenderPre(RenderPlayerEvent.Pre event) {
        Player rendered = event.getEntity();
        if (isThunderAccelActive(rendered.getUUID()) && rendered != Minecraft.getInstance().player) {
            event.setCanceled(true);
            return;
        }
        if (PURE_FIRE_MEDITATING && event.getEntity() == Minecraft.getInstance().player) {
            var model = event.getRenderer().getModel();
            // 精确对应 冥想打坐.bbmodel 的打坐姿态参数:
            // Waist/Body: 下沉约 10 个像素高度
            // Right Arm: rot(44°, -30°, -10°) -> 弧度 (0.768, -0.524, -0.175)
            // Left Arm: rot(44°, 30°, 10°) -> 弧度 (0.768, 0.524, 0.175)
            // Right Leg: rot(80°, 52°, 26°) -> 盘腿 弧度 (1.396, 0.908, 0.454)
            // Left Leg: rot(80°, -52°, -26°) -> 盘腿 弧度 (1.396, -0.908, -0.454)
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0D, 0.625D, 0.0D); // y: -10像素对应的下沉

            model.rightArm.xRot = 0.768F;
            model.rightArm.yRot = -0.524F;
            model.rightArm.zRot = -0.175F;

            model.leftArm.xRot = 0.768F;
            model.leftArm.yRot = 0.524F;
            model.leftArm.zRot = 0.175F;

            model.rightLeg.xRot = 1.396F;
            model.rightLeg.yRot = 0.908F;
            model.rightLeg.zRot = 0.454F;

            model.leftLeg.xRot = 1.396F;
            model.leftLeg.yRot = -0.908F;
            model.leftLeg.zRot = -0.454F;
        }
        if (!event.getEntity().hasEffect(ModEffects.SEVERING.get())) return;
        var model = event.getRenderer().getModel();
        model.leftLeg.visible = false;
        model.rightLeg.visible = false;
    }

    @SubscribeEvent
    public static void onSeveringRenderPost(RenderPlayerEvent.Post event) {
        var model = event.getRenderer().getModel();
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        if (PURE_FIRE_MEDITATING && event.getEntity() == Minecraft.getInstance().player) {
            model.leftArm.xRot = 0.0F;
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = 0.0F;
            model.rightArm.xRot = 0.0F;
            model.rightArm.yRot = 0.0F;
            model.rightArm.zRot = 0.0F;
            model.leftLeg.xRot = 0.0F;
            model.leftLeg.yRot = 0.0F;
            model.leftLeg.zRot = 0.0F;
            model.rightLeg.xRot = 0.0F;
            model.rightLeg.yRot = 0.0F;
            model.rightLeg.zRot = 0.0F;
            event.getPoseStack().popPose();
        }

        // Skills 1-4 render custom effects on the first-person pass
        // (vortex, gauntlets, punch); later skills stay vanilla.
        if (FirstPersonMode.isFirstPersonPass()
                && event.getEntity() == Minecraft.getInstance().player
                && event.getEntity() instanceof AbstractClientPlayer localPlayer) {
            PureFireHandsLayer.renderFirstPerson(event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight(), localPlayer,
                    event.getRenderer().getModel(), event.getPartialTick(),
                    localPlayer.tickCount + event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onPureFirePunchFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()
                || event.getCamera().getEntity() != mc.player
                || mc.player.level() == null) return;
        AbstractClientPlayer localPlayer = mc.player;
        List<PureFirePunchEntity> punches = localPlayer.level().getEntitiesOfClass(
                PureFirePunchEntity.class, localPlayer.getBoundingBox().inflate(32.0D),
                punch -> punch.getOwner() == localPlayer && !punch.isRemoved());
        if (punches.isEmpty()) return;
        PureFirePunchEntity punch = punches.get(0);
        if (punch.getPhase() != 0) return;
        float charge = punch.getPhaseProgress((float) event.getPartialTick());
        float zoom = 1.0F - 0.16F * (charge * charge * (3.0F - 2.0F * charge));
        event.setFOV(event.getFOV() * zoom);
    }

    @SubscribeEvent
    public static void onSeveringCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()
                || !mc.player.hasEffect(ModEffects.SEVERING.get())
                || event.getCamera().getEntity() != mc.player) return;
        try {
            java.lang.reflect.Method setter = null;
            for (java.lang.reflect.Method method : event.getCamera().getClass().getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 1 && parameters[0] == Vec3.class) {
                    setter = method;
                    break;
                }
            }
            if (setter == null) return;
            setter.setAccessible(true);
            Vec3 current = event.getCamera().getPosition();
            setter.invoke(event.getCamera(), new Vec3(current.x, mc.player.getY() + 1.0D, current.z));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    /** 开启雷法时从脚下炸开一圈水花，大约 1.2 秒散开。 */
    private static void tickThunderAccelWave(Minecraft mc) {
        if (THUNDER_ACCEL_WAVE_TICKS <= 0 || mc.level == null || mc.player == null) return;
        THUNDER_ACCEL_WAVE_TICKS--;
        THUNDER_ACCEL_WAVE_RADIUS += 0.85F;
        double y = mc.player.getY() + 0.15D;
        int points = 28;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = mc.player.getX() + Math.cos(angle) * THUNDER_ACCEL_WAVE_RADIUS;
            double z = mc.player.getZ() + Math.sin(angle) * THUNDER_ACCEL_WAVE_RADIUS;
            mc.level.addParticle(ParticleTypes.SPLASH, x, y, z, 0.0D, 0.08D, 0.0D);
            if (i % 4 == 0) {
                mc.level.addParticle(ParticleTypes.CLOUD, x, y + 0.05D, z, 0.0D, 0.02D, 0.0D);
            }
        }
    }
}
