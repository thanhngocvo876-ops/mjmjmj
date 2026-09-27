package com.mjmod.client.renderer;

import com.mjmod.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * DragonBall-style power-up aura rendered as two large crossed translucent
 * flame blades attached to the player. Only visible in third person or for
 * remote players (first person does not render PlayerRenderer layers), which
 * keeps the local camera unobstructed. Frame animation flows upward via the
 * four generated flame-sheet textures.
 */
public class PureFireAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation[] FRAMES = {
            new ResourceLocation("mjmod", "textures/effect/pure_fire_aura_sheet_0.png"),
            new ResourceLocation("mjmod", "textures/effect/pure_fire_aura_sheet_1.png"),
            new ResourceLocation("mjmod", "textures/effect/pure_fire_aura_sheet_2.png"),
            new ResourceLocation("mjmod", "textures/effect/pure_fire_aura_sheet_3.png")
    };
    private static final float HALF_WIDTH = 0.85F;
    private static final float BLADE_HEIGHT = 2.6F;
    // LivingEntityRenderer's model space uses +Y downward: the player's feet
    // are at local Y=1.5 and points above the head use negative local Y.
    private static final float PLAYER_FEET_Y = 1.5F;
    private static final float BASE_ALPHA = 0.62F;
    private static final int FRAME_TICKS = 3;

    public PureFireAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        MobEffectInstance effect = firstPureFireEffect(player);
        if (effect == null) {
            return;
        }

        float time = player.tickCount + partialTick;
        int frame = ((int) (time / FRAME_TICKS)) % FRAMES.length;
        int counterFrame = (frame + FRAMES.length / 2) % FRAMES.length;
        float swayDeg = Mth.sin(ageInTicks * 0.08F) * 4.0F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(225.0F + swayDeg));
        renderBlade(poseStack, buffer, FRAMES[frame]);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(135.0F - swayDeg));
        renderBlade(poseStack, buffer, FRAMES[counterFrame]);
        poseStack.popPose();
    }

    private static MobEffectInstance firstPureFireEffect(AbstractClientPlayer player) {
        MobEffectInstance effect = player.getEffect(ModEffects.PURE_FIRE_SCHEME_A.get());
        if (effect != null) return effect;
        return player.getEffect(ModEffects.PURE_FIRE_SCHEME_D.get());
    }

    private void renderBlade(PoseStack poseStack, MultiBufferSource buffer,
                             ResourceLocation texture) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        float alpha = BASE_ALPHA;

        // Draw from the feet upward. Using 0..+height here makes the flame run
        // from the player's head down through the ground because model +Y is down.
        float topY = PLAYER_FEET_Y - BLADE_HEIGHT;
        vertex(pose, vc, -HALF_WIDTH, PLAYER_FEET_Y, 0, 1, light, overlay, alpha);
        vertex(pose, vc, HALF_WIDTH, PLAYER_FEET_Y, 1, 1, light, overlay, alpha);
        vertex(pose, vc, HALF_WIDTH, topY, 1, 0, light, overlay, alpha);
        vertex(pose, vc, -HALF_WIDTH, topY, 0, 0, light, overlay, alpha);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc,
                               float x, float y, int u, int v,
                               int light, int overlay, float alpha) {
        vc.vertex(pose.pose(), x, y, 0.0F)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}
