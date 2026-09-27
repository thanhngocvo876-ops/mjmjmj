package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearShotModel;
import com.mjmod.entity.VoidTearShotEntity;
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

/** 水盾发射的长泪光束，视觉与一技能保持一致。 */
public class VoidTearShotRenderer extends EntityRenderer<VoidTearShotEntity> {
    /** Keep the same authored length as the first-skill beam. */
    private static final float MODEL_LENGTH_BLOCKS = 20.8F / 16.0F;
    private static final float RAIL_LENGTH_BLOCKS = 35.0F;
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/void_tear_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
            "forge", "textures/white.png");
    private final VoidTearShotModel model;

    public VoidTearShotRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new VoidTearShotModel(context.bakeLayer(VoidTearShotModel.LAYER_LOCATION));
    }

    /** A moving laser rail must remain visible even when its small hitbox is
     * outside the camera frustum while the rail itself crosses the screen. */
    @Override
    public boolean shouldRender(VoidTearShotEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(VoidTearShotEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 motion = entity.getShotDirection();
        poseStack.pushPose();
        float yaw = (float) Math.atan2(motion.x, motion.z);
        float pitch = (float) Math.atan2(motion.y,
                Math.sqrt(motion.x * motion.x + motion.z * motion.z));
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(-pitch));
        // Exact visual copy of the first-skill light rail.  The second skill
        // keeps its own model/entity, but uses the same outer halo, core,
        // colour and 35-block straight track.  The entity itself is already
        // placed at the rail midpoint by VoidTearShotEntity, so do not add a
        // second forward translation here: the beam's root must stay flush
        // with the water-shield surface.
        float pulse = 1.0F + (float) Math.sin((entity.tickCount + partialTick) * 0.65F) * 0.05F;
        float alpha = entity.getFadeAlpha(partialTick);
        poseStack.scale(1.15F * pulse, 1.15F * pulse,
                RAIL_LENGTH_BLOCKS / MODEL_LENGTH_BLOCKS);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        VertexConsumer white = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.18F, 1.18F, 1.0F);
        model.renderOuter(poseStack, white, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.72F * alpha);
        poseStack.popPose();
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

    @Override
    public ResourceLocation getTextureLocation(VoidTearShotEntity entity) {
        return TEXTURE;
    }
}
