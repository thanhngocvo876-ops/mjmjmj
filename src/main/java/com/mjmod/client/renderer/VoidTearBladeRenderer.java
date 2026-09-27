package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearBladeModel;
import com.mjmod.entity.VoidTearBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 泪刃实体渲染：先由小到大成形，再白包蓝发光并渐隐。 */
public class VoidTearBladeRenderer extends EntityRenderer<VoidTearBladeEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/void_tear_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
            "forge", "textures/white.png");
    private final VoidTearBladeModel model;

    public VoidTearBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new VoidTearBladeModel(context.bakeLayer(VoidTearBladeModel.LAYER_LOCATION));
    }

    @Override
    public boolean shouldRender(VoidTearBladeEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(VoidTearBladeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 direction = entity.getBladeDirection();
        float yaw = (float) Math.atan2(direction.x, direction.z);
        float construct = entity.getConstructionProgress(partialTick);
        float alpha = entity.getFadeAlpha(partialTick);
        float pulse = 1.0F + (float) Math.sin((entity.tickCount + partialTick) * 0.5F) * 0.04F;
        // During charge keep a compact eye-sized sword aura, ending at one
        // block high. Once flight begins, grow smoothly from one to four blocks
        // over the first 20 flight ticks. Width/depth grow more subtly.
        float heightScale = entity.isCharging()
                ? 0.10F + construct * 0.15F
                : 0.25F + construct * 0.75F;
        float breadthScale = entity.isCharging()
                ? 0.20F + construct * 0.22F
                : 0.42F + construct * 0.58F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(yaw));
        // Keep the blade vertical in world space for a true top-to-bottom cut;
        // only its travel direction follows the player's vertical aim.
        poseStack.scale(breadthScale * pulse, heightScale * pulse, breadthScale * pulse);
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);

        // Leave several offset silhouettes behind the moving blade. Each one
        // is a translucent copy of the chunky Blockbench geometry, so the
        // launch reads as a continuous tear-energy trail rather than an arrow.
        if (!entity.isCharging()) {
            VertexConsumer trail = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            for (int i = 4; i >= 1; i--) {
                float trailFade = (5.0F - i) / 5.0F;
                float trailScale = 0.72F + trailFade * 0.22F;
                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, -i * 0.72F);
                poseStack.scale(trailScale, trailScale, trailScale);
                model.renderOuter(poseStack, trail, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, 0.30F, 0.70F, 1.0F,
                        (0.10F + trailFade * 0.08F) * entity.getFadeAlpha(partialTick));
                poseStack.popPose();
            }
        }

        VertexConsumer white = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.16F, 1.04F, 1.18F);
        model.renderOuter(poseStack, white, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.70F * alpha);
        poseStack.popPose();

        VertexConsumer blue = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        poseStack.pushPose();
        // Keep the blue pass inside the larger white sheath so the crescent
        // reads as white-wrapped-blue instead of a blue silhouette covering it.
        poseStack.scale(1.04F, 1.00F, 1.0F);
        model.renderOuter(poseStack, blue, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.34F, 0.72F, 1.0F, 0.45F * alpha);
        poseStack.popPose();
        model.renderCore(poseStack, blue, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.86F, 0.97F, 1.0F, 0.96F * alpha);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTearBladeEntity entity) {
        return TEXTURE;
    }
}
