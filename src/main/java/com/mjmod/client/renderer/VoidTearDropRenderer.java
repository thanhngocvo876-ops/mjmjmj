package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearDropModel;
import com.mjmod.entity.VoidTearDropEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 自动护卫水珠渲染：白色水膜包裹淡蓝核心。 */
public class VoidTearDropRenderer extends EntityRenderer<VoidTearDropEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/void_tear_beam.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
            "forge", "textures/white.png");
    private static final float MODEL_SCALE = 1.12F;
    private final VoidTearDropModel model;

    public VoidTearDropRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new VoidTearDropModel(context.bakeLayer(VoidTearDropModel.LAYER_LOCATION));
        shadowRadius = 0.08F;
    }

    @Override
    public void render(VoidTearDropEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = entity.tickCount + partialTick;
        float form = entity.getFormationProgress(partialTick);
        float scale = MODEL_SCALE * (0.16F + form * 0.84F)
                * (1.0F + Mth.sin(time * 0.24F) * 0.035F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(time * 0.035F));
        if (entity.getState() == VoidTearDropEntity.STATE_FLYING) {
            Vec3 direction = entity.getFlightDirection();
            float yaw = (float) Math.atan2(direction.x, direction.z);
            float pitch = (float) Math.atan2(direction.y,
                    Math.sqrt(direction.x * direction.x + direction.z * direction.z));
            poseStack.mulPose(Axis.YP.rotation(yaw));
            poseStack.mulPose(Axis.XP.rotation(-pitch));
            // The projectile remains a round water bead; its trail is made of
            // water particles rather than stretching the bead into an arrow.
            poseStack.scale(0.98F, 0.98F, 0.98F);
        }
        poseStack.scale(scale, scale, scale);
        model.setupAnim(entity, 0.0F, 0.0F, time, 0.0F, 0.0F);

        VertexConsumer white = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.10F, 1.10F, 1.10F);
        model.renderOuter(poseStack, white, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 0.36F);
        poseStack.popPose();

        // Tint a white translucent surface directly so the blue core stays
        // visible through the white shell instead of becoming pale/white.
        VertexConsumer blue = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        model.renderOuter(poseStack, blue, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0.30F, 0.72F, 1.0F, 0.78F);
        model.renderCore(poseStack, blue, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0.72F, 0.95F, 1.0F, 0.96F);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTearDropEntity entity) {
        return TEXTURE;
    }
}
