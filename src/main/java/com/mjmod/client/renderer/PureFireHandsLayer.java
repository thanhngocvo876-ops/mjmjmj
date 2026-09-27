package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.ClientEvents;
import com.mjmod.client.model.PureFireGauntletModel;
import com.mjmod.client.model.PureFireBladeModel;
import com.mjmod.entity.PureFirePunchEntity;
import com.mjmod.entity.PureFireVortexEntity;
import com.mjmod.entity.PureFireCleaveEntity;
import com.mjmod.entity.PureFireThrustEntity;
import com.mjmod.entity.PureFireMeteorDiveEntity;
import com.mjmod.registry.ModEffects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.impl.IUpperPartHelper;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.IBendHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;



/** Renders the fire-fist model, punch and skill-1 beam on live arm bones. */
public final class PureFireHandsLayer extends RenderLayer<AbstractClientPlayer,
        PlayerModel<AbstractClientPlayer>> implements IUpperPartHelper {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/effect/pure_fire_flame_32.png");
    private static final ResourceLocation GAUNTLET_TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/effect/pure_fire_gauntlet.png");
    private static final ResourceLocation[] AURA_SHEETS = {
            new ResourceLocation(MJMod.MODID, "textures/effect/pure_fire_aura_sheet_0.png"),
            new ResourceLocation(MJMod.MODID, "textures/effect/pure_fire_aura_sheet_1.png"),
            new ResourceLocation(MJMod.MODID, "textures/effect/pure_fire_aura_sheet_2.png"),
            new ResourceLocation(MJMod.MODID, "textures/effect/pure_fire_aura_sheet_3.png")
    };
    private final PureFireBladeModel bladeModel;
    private final PureFireGauntletModel gauntletModel;

    /**
     * First-person pass for skills 1-4. The player-anim THIRD_PERSON_MODEL
     * pass renders the full player model, so the same layer draws gauntlets,
     * punch, blade and vortex exactly like third person.
     */
    public static void renderFirstPerson(PoseStack poseStack, MultiBufferSource buffer,
                                         int packedLight, AbstractClientPlayer player,
                                         PlayerModel<AbstractClientPlayer> parentModel,
                                         float partialTick, float ageInTicks) {
        PureFireHandsLayer activeLayer = ClientFirstPersonHolder.get(parentModel);
        if (activeLayer != null) {
            activeLayer.renderHands(poseStack, buffer, packedLight, player, parentModel,
                    partialTick, ageInTicks, true);
        }
    }

    private static final class ClientFirstPersonHolder {
        private static final java.util.Map<PlayerModel<?>, PureFireHandsLayer> LAYERS =
                new java.util.WeakHashMap<>();

        private static PureFireHandsLayer get(PlayerModel<?> model) {
            return LAYERS.get(model);
        }
    }

    public PureFireHandsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                              EntityModelSet modelSet) {
        super(parent);
        bladeModel = new PureFireBladeModel(modelSet.bakeLayer(PureFireBladeModel.LAYER_LOCATION));
        gauntletModel = new PureFireGauntletModel(modelSet.bakeLayer(PureFireGauntletModel.LAYER_LOCATION));
        ClientFirstPersonHolder.LAYERS.put(parent.getModel(), this);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        renderHands(poseStack, buffer, packedLight, player, getParentModel(), partialTick,
                ageInTicks, false);
    }

    private void renderHands(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                             AbstractClientPlayer player,
                             PlayerModel<AbstractClientPlayer> parentModel,
                             float partialTick, float ageInTicks,
                             boolean applyBodyAnimationTransform) {
        PureFirePunchEntity punch = findPunch(player);
        PureFireVortexEntity flame = findFlame(player);
        PureFireCleaveEntity cleave = findCleave(player);
        PureFireThrustEntity thrust = findThrust(player);
        com.mjmod.entity.PureFireWallCastEntity wallCast = findWallCast(player);
        PureFireMeteorDiveEntity dive = findMeteorDive(player);
        boolean handForm = player == Minecraft.getInstance().player
                ? ClientEvents.PURE_FIRE_HAND_FORM
                : player.hasEffect(ModEffects.PURE_FIRE_HAND_FORM.get());
        boolean bladeHold = player.hasEffect(ModEffects.PURE_FIRE_BLADE_HOLD.get());
        boolean hasBlade = cleave != null || thrust != null || wallCast != null || bladeHold;
        // 技能 4（增压）期间同样显示拳套
        boolean showGauntlets = (handForm || dive != null) && punch == null && !hasBlade;
        if (punch == null && (flame == null || !flame.isReleased()) && !showGauntlets && !hasBlade) return;

        // Player Animator applies the body bend around the upper-body parts
        // while vanilla renders the player model.  This custom layer is drawn
        // separately, so the first-person fallback must apply the same matrix
        // transform explicitly.  The normal third-person path receives it from
        // LivingEntityRenderRedirect_bendOnly because this layer implements
        // IUpperPartHelper below.
        boolean bodyTransformPushed = false;
        if (applyBodyAnimationTransform && player instanceof IAnimatedPlayer animatedPlayer) {
            var animation = animatedPlayer.playerAnimator_getAnimation();
            if (animation.isActive()) {
                poseStack.pushPose();
                IBendHelper.rotateMatrixStack(poseStack, animation.getBend("body"));
                bodyTransformPushed = true;
            }
        }

        try {
            if (showGauntlets) {
                renderHandForm(poseStack, buffer, parentModel, ageInTicks);
            }
            if (punch != null) {
                renderStraightPunchModel(poseStack, buffer, parentModel, punch, partialTick,
                        ageInTicks);
            }
            if (hasBlade) {
                renderBladeWithAura(poseStack, buffer, player, parentModel, cleave, thrust,
                        wallCast, bladeHold, partialTick, ageInTicks);
            }
            if (flame != null && flame.isReleased()) {
                renderVortex(poseStack, buffer, player, parentModel, flame, partialTick, ageInTicks);
            }
        } finally {
            if (bodyTransformPushed) poseStack.popPose();
        }
    }

    /** Renders the new gauntlet model on both live arm bones during 直冲拳. */
    private void renderStraightPunchModel(PoseStack poseStack, MultiBufferSource buffer,
                                          PlayerModel<AbstractClientPlayer> parentModel,
                                          PureFirePunchEntity punch, float partialTick,
                                          float ageInTicks) {
        float clip = punch.getModelAnimationProgress(partialTick);
        gauntletModel.animatePunch(clip, punch.getPhase(), ageInTicks);
        VertexConsumer gauntlet = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(GAUNTLET_TEXTURE));
        renderGauntletArm(poseStack, parentModel.leftArm, true, gauntletModel, gauntlet);
        renderGauntletArm(poseStack, parentModel.rightArm, false, gauntletModel, gauntlet);
    }

    /** Skill 2 / skill 4 gauntlet with the flowing-flame animation. */
    private void renderHandForm(PoseStack poseStack, MultiBufferSource buffer,
                                PlayerModel<AbstractClientPlayer> parentModel,
                                float ageInTicks) {
        gauntletModel.animate(ageInTicks);
        VertexConsumer gauntlet = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(GAUNTLET_TEXTURE));
        renderGauntletArm(poseStack, parentModel.leftArm, true, gauntletModel, gauntlet);
        renderGauntletArm(poseStack, parentModel.rightArm, false, gauntletModel, gauntlet);
    }

    private static void renderGauntletArm(PoseStack poseStack, ModelPart arm,
                                          boolean left, PureFireGauntletModel model,
                                          VertexConsumer gauntlet) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        // The Blockbench file was authored on the player model: move from the
        // vanilla upper-arm pivot to the hand end, then the tuned side
        // correction. The first-person THIRD_PERSON_MODEL pass renders the
        // same geometry, so the same transform applies there too.
        poseStack.translate(0.0F, 10.0F / 16.0F, 0.0F);
        poseStack.translate(left ? 3.0F / 16.0F : -3.0F / 16.0F,
                -2.0F / 16.0F, 0.0F);
        if (left) {
            model.renderLeft(poseStack, gauntlet, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            model.renderRight(poseStack, gauntlet, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();
    }

    private static PureFireMeteorDiveEntity findMeteorDive(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(PureFireMeteorDiveEntity.class,
                player.getBoundingBox().inflate(48.0D),
                dive -> dive.getOwner() == player && !dive.isRemoved())
                .stream().findFirst().orElse(null);
    }


    /** Marks this layer as belonging to the Player Animator upper-body pass. */
    @Override
    public boolean isUpperPart() {
        return true;
    }

    /** The layer's classification is fixed; ignore external toggles. */
    @Override
    public void setUpperPart(boolean upperPart) {
    }

    private static PureFireCleaveEntity findCleave(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(PureFireCleaveEntity.class,
                player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findFirst().orElse(null);
    }

    private static PureFireThrustEntity findThrust(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(PureFireThrustEntity.class,
                player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findFirst().orElse(null);
    }

    private static com.mjmod.entity.PureFireWallCastEntity findWallCast(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(com.mjmod.entity.PureFireWallCastEntity.class,
                player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findFirst().orElse(null);
    }

    /**
     * Renders the Fire Blade model attached to the player's right arm,
     * along with mini crossed aura sheets rising from the blade edge,
     * and samples tip world coordinates for ribbon trail rendering.
     */
    private void renderBladeWithAura(PoseStack poseStack, MultiBufferSource buffer,
                                    AbstractClientPlayer player,
                                    PlayerModel<AbstractClientPlayer> parentModel,
                                    PureFireCleaveEntity cleave,
                                    PureFireThrustEntity thrust,
                                    com.mjmod.entity.PureFireWallCastEntity wallCast,
                                    boolean bladeHold,
                                    float partialTick, float ageInTicks) {
        float appear = bladeAppear(cleave, thrust, wallCast, bladeHold, partialTick);
        if (appear <= 0.001F) return;
        float intensity = cleave != null ? 1.5F : (thrust != null ? 1.8F : (wallCast != null ? 1.4F : 1.0F));
        bladeModel.animate(ageInTicks, intensity);

        VertexConsumer vc = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(PureFireBladeModel.TEXTURE));

        poseStack.pushPose();
        parentModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.0F, (1.0F - appear) * 0.55F, 0.0F);
        poseStack.scale(appear, appear, appear);

        // Render the 30-cube blade
        bladeModel.render(poseStack, vc, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, appear);

        // Render Mini Crossed Aura Sheets on the blade
        renderMiniBladeAura(poseStack, buffer, ageInTicks, partialTick);

        // Record blade tip for ribbon trail - removed to prevent vertex stretching glitches
        poseStack.popPose();
    }

    /** 前 8 tick 从掌心长出，留手阶段保持满刃。 */
    private static float bladeAppear(PureFireCleaveEntity cleave, PureFireThrustEntity thrust,
                                     com.mjmod.entity.PureFireWallCastEntity wallCast,
                                     boolean bladeHold, float partialTick) {
        if (bladeHold && cleave == null && thrust == null && wallCast == null) return 1.0F;
        int ticks = cleave != null ? cleave.tickCount
                : thrust != null ? thrust.tickCount
                : wallCast != null ? wallCast.tickCount : 8;
        return Mth.clamp((ticks + partialTick) / 8.0F, 0.0F, 1.0F);
    }

    private void renderMiniBladeAura(PoseStack poseStack, MultiBufferSource buffer,
                                    float ageInTicks, float partialTick) {
        float time = ageInTicks + partialTick;
        int frame = ((int) (time / 3.0F)) % AURA_SHEETS.length;
        int counterFrame = (frame + AURA_SHEETS.length / 2) % AURA_SHEETS.length;

        float bladeH = 0.85F;
        float bladeW = 0.18F;

        poseStack.pushPose();
        poseStack.translate(0.0F, 10.0F / 16.0F, 0.0F);

        // Crossed sheet 1
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        renderMiniQuad(poseStack, buffer, AURA_SHEETS[frame], bladeW, bladeH);
        poseStack.popPose();

        // Crossed sheet 2
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
        renderMiniQuad(poseStack, buffer, AURA_SHEETS[counterFrame], bladeW, bladeH);
        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderMiniQuad(PoseStack poseStack, MultiBufferSource buffer,
                                ResourceLocation tex, float w, float h) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        float a = 0.68F;

        vertex(vc, pose, -w, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, a, 0.0F, 1.0F, light, overlay);
        vertex(vc, pose, w, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, a, 1.0F, 1.0F, light, overlay);
        vertex(vc, pose, w, -h, 0.0F, 1.0F, 1.0F, 1.0F, a, 1.0F, 0.0F, light, overlay);
        vertex(vc, pose, -w, -h, 0.0F, 1.0F, 1.0F, 1.0F, a, 0.0F, 0.0F, light, overlay);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose,
                               float x, float y, float z, float r, float g, float b, float a,
                               float u, float v, int light, int overlay) {
        vc.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static PureFirePunchEntity findPunch(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(PureFirePunchEntity.class,
                player.getBoundingBox().inflate(3.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findFirst().orElse(null);
    }

    private static PureFireVortexEntity findFlame(AbstractClientPlayer player) {
        if (player.level() == null) return null;
        return player.level().getEntitiesOfClass(PureFireVortexEntity.class,
                player.getBoundingBox().inflate(32.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findFirst().orElse(null);
    }

    /** Draws a fire-coloured laser column from the animated right-arm bone. */
    private void renderVortex(PoseStack poseStack, MultiBufferSource buffer,
                              AbstractClientPlayer player,
                              PlayerModel<AbstractClientPlayer> parentModel,
                              PureFireVortexEntity flame, float partialTick,
                              float ageInTicks) {
        float length = flame.getLength();
        float alpha = flame.getAlpha(partialTick);
        int stage = flame.getDamage() <= 5.0F ? 1 : flame.getDamage() <= 10.0F ? 2 : 3;

        poseStack.pushPose();
        parentModel.rightArm.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 10.0F / 16.0F, 0.0F);
        // The right-arm animation itself tracks the live crosshair. Mapping
        // local +Z onto the palm axis keeps the beam attached without applying
        // a second yaw/pitch rotation on top of the animated arm.
        poseStack.mulPose(Axis.XP.rotation(-Mth.HALF_PI));
        VertexConsumer outer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        VertexConsumer core = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        float pulse = 1.0F + Mth.sin((ageInTicks + partialTick) * 0.45F) * 0.06F;
        float outerRadius = (0.16F + stage * 0.035F) * pulse;
        float coreRadius = (0.075F + stage * 0.018F) * pulse;
        renderLaserTube(poseStack, outer, length, outerRadius,
                new Color(0.98F, 0.16F, 0.015F, (0.45F + stage * 0.06F) * alpha),
                ageInTicks * 0.12F, 10);
        renderLaserTube(poseStack, core, length, coreRadius,
                new Color(1.0F, 0.92F, 0.42F, (0.82F + stage * 0.05F) * alpha),
                -ageInTicks * 0.18F, 8);
        poseStack.popPose();
    }

    private static void renderLaserTube(PoseStack poseStack, VertexConsumer consumer,
                                        float length, float radius,
                                        Color color, float phase, int sides) {
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < sides; i++) {
            float a0 = phase + Mth.TWO_PI * i / sides;
            float a1 = phase + Mth.TWO_PI * (i + 1) / sides;
            float x0 = Mth.cos(a0) * radius;
            float y0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float y1 = Mth.sin(a1) * radius;
            float nx = Mth.cos((a0 + a1) * 0.5F);
            float ny = Mth.sin((a0 + a1) * 0.5F);
            vertex(consumer, pose, x0, y0, 0.0F, color, 0.0F, 0.0F, nx, ny, 0.0F);
            vertex(consumer, pose, x1, y1, 0.0F, color, 1.0F / sides, 0.0F, nx, ny, 0.0F);
            vertex(consumer, pose, x1, y1, length, color, 1.0F / sides, 1.0F, nx, ny, 0.0F);
            vertex(consumer, pose, x0, y0, length, color, 0.0F, 1.0F, nx, ny, 0.0F);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float z, Color color,
                               float u, float v, float nx, float ny, float nz) {
        consumer.vertex(pose.pose(), x, y, z)
                .color(color.red(), color.green(), color.blue(), color.alpha())
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private record Color(float red, float green, float blue, int alpha) {
        private Color(float red, float green, float blue, float alpha) {
            this(red, green, blue, (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F));
        }
    }

}
