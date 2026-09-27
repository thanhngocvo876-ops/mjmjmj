package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearShieldModel;
import com.mjmod.entity.VoidTearShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;

/** 蓄力水盾渲染：逐渐成形，并在发射时淡出。 */
public class VoidTearShieldRenderer extends EntityRenderer<VoidTearShieldEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/void_tear_shield.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation(
            "forge", "textures/white.png");
    private final VoidTearShieldModel model;

    public VoidTearShieldRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new VoidTearShieldModel(Minecraft.getInstance().getEntityModels()
                .bakeLayer(VoidTearShieldModel.LAYER_LOCATION));
    }

    @Override
    public void render(VoidTearShieldEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float progress = entity.getChargeProgress(partialTick);
        float alpha = entity.getFadeAlpha(partialTick);
        float time = entity.tickCount + partialTick;
        Vec3 facing = entity.getFacingDirection();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.05F, 0.0F);
        // The water wall is a front-facing plane. Rotate the model to the
        // direction captured when Z was pressed instead of drawing a box around
        // the player.
        float yaw = (float) Math.atan2(facing.x, facing.z);
        float pitch = (float) Math.atan2(facing.y,
                Math.sqrt(facing.x * facing.x + facing.z * facing.z));
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(-pitch));
        float scale = 0.18F + progress * 0.82F;
        poseStack.scale(scale, scale, scale);
        model.animate(time, progress);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        VertexConsumer white = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.075F, 1.075F, 1.075F);
        model.renderToBuffer(poseStack, white, 15728880, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.56F * alpha);
        poseStack.popPose();
        model.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY,
                0.34F, 0.80F, 1.0F, 0.42F * alpha);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(VoidTearShieldEntity entity) {
        return TEXTURE;
    }
}
