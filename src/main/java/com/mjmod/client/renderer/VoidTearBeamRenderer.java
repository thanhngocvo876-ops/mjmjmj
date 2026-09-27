package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearBeamModel;
import com.mjmod.entity.VoidTearBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 将长泪流模型沿飞行方向对准并渲染。 */
public class VoidTearBeamRenderer extends EntityRenderer<VoidTearBeamEntity> {
    private static final float MODEL_LENGTH_BLOCKS = 20.8F / 16.0F;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/void_tear_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
            "forge", "textures/white.png");
    private final VoidTearBeamModel model;

    public VoidTearBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new VoidTearBeamModel(context.bakeLayer(VoidTearBeamModel.LAYER_LOCATION));
    }

    /** The visible beam is much longer than its tiny logical hitbox. */
    @Override
    public boolean shouldRender(VoidTearBeamEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(VoidTearBeamEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getBeamDirection();
        poseStack.pushPose();
        float yaw = (float) Math.atan2(motion.x, motion.z);
        float pitch = (float) Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z));
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(-pitch));
        float length = entity.getBeamLength();
        float appear = entity.getAppearScale(partialTick);
        poseStack.translate(0.0F, 0.0F, (appear - 1.0F) * length * 0.5F);
        float pulse = 1.0F + (float) Math.sin((entity.tickCount + partialTick) * 0.65F) * 0.035F;
        poseStack.scale(1.15F * pulse, 1.15F * pulse,
                length / MODEL_LENGTH_BLOCKS * appear);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float alpha = entity.getFadeAlpha(partialTick);

        // Draw a narrow white sheath first, then the original blue outer/core
        // passes. This preserves the existing laser silhouette and fade while
        // producing the requested white-wrapped-blue colour treatment.
        VertexConsumer white = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.18F, 1.18F, 1.0F);
        model.renderOuter(poseStack, white, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.72F * alpha);
        poseStack.popPose();

        // The beam is viewed almost head-on in first person.  Keep a readable
        // halo around the Blockbench geometry so it does not collapse to a
        // one-pixel dot while still remaining a narrow laser.
        poseStack.pushPose();
        poseStack.scale(2.55F, 2.55F, 1.0F);
        model.renderOuter(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.34F, 0.72F, 1.0F, 0.30F * alpha);
        poseStack.popPose();

        model.renderOuter(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.46F, 0.80F, 1.0F, 0.68F * alpha);
        model.renderCore(poseStack, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                0.86F, 0.97F, 1.0F, 0.94F * alpha);
        poseStack.popPose();
    }

    @Override public ResourceLocation getTextureLocation(VoidTearBeamEntity entity) { return TEXTURE; }
}
