package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.ClientEvents;
import com.mjmod.client.model.VoidTearsHelmetModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player-model layer for the void tears helmet.
 *
 * The layer is invoked while vanilla is still inside the player's render
 * transform. Translating the parent's head part therefore keeps the accessory
 * attached to the actual head pivot for both classic and slim player models.
 */
public final class VoidTearsHelmetLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MJMod.MODID, "textures/entity/void_tears_helmet.png");
    /** Forge's solid white texture gives the liquid a soft white shell without
     * recolouring the authored blue core texture. */
    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation("forge", "textures/white.png");

    private final VoidTearsHelmetModel model;

    public VoidTearsHelmetLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                EntityModelSet modelSet) {
        super(parent);
        this.model = new VoidTearsHelmetModel(modelSet.bakeLayer(VoidTearsHelmetModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        // This is a permission-driven cosmetic layer, not an armor layer.
        // The model remains hidden until the player uses an onion to activate
        // the artifact, regardless of what is in the helmet slot.
        // Active state is currently synchronized to the local client only;
        // keep the layer local so activating it never makes every nearby
        // player render the same eye accessory.
        if (player != Minecraft.getInstance().player) return;
        if (!ClientEvents.VOID_TEARS_ACTIVE) return;

        poseStack.pushPose();

        // Vanilla has already applied body rotation, model mirroring, scale and
        // the model origin at this point. This is the exact head pivot, including
        // the player's live head yaw/pitch and crouching animation.
        getParentModel().head.translateAndRotate(poseStack);
        // Keep the effect just outside the head's front surface like an armor
        // overlay, then add a subtle upward drift to the whole liquid mass.
        // The authored tail sections are deliberately deep enough to look
        // three-dimensional. Push the whole accessory beyond the head's front
        // plane so its back faces cannot sink into the player's skull when the
        // white outer pass is scaled up or the head is pitched.
        poseStack.translate(0.0F, -0.020F + Mth.sin(ageInTicks * 0.16F) * 0.012F, -0.13F);

        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // The parent head transform above owns head rotation. The accessory model
        // head is only a container for its local parts, so avoid rotating twice.
        model.head.xRot = 0.0F;
        model.head.yRot = 0.0F;
        model.head.zRot = 0.0F;

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.18F) * 0.035F;

        // White outer shell, slightly larger than the blue authored geometry.
        // The two passes intentionally share the same animated model so the
        // white edge hugs the eye and upper-left flowing plume precisely.
        VertexConsumer whiteConsumer = buffer.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        poseStack.pushPose();
        poseStack.scale(1.105F, 1.105F, 1.105F);
        model.renderToBuffer(poseStack, whiteConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 0.62F);
        poseStack.popPose();

        // A second translucent pass creates the moving liquid glow without
        // changing the authored silhouette or its right-eye anchor.
        poseStack.pushPose();
        float flow = (Mth.sin(ageInTicks * 0.24F + 0.7F) + 1.0F) * 0.5F;
        poseStack.translate(-0.035F - flow * 0.095F,
                -0.030F - flow * 0.105F,
                -0.018F + Mth.sin(ageInTicks * 0.31F) * 0.012F);
        float glowScale = 1.055F + Mth.sin(ageInTicks * 0.14F + 1.2F) * 0.025F;
        poseStack.scale(glowScale, glowScale, glowScale);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                0.35F, 0.78F, 1.0F, 0.18F + flow * 0.12F);
        poseStack.popPose();

        poseStack.scale(pulse, 1.0F + Mth.sin(ageInTicks * 0.21F + 0.5F) * 0.045F, pulse);
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY,
                0.9F, 0.98F, 1.0F, 0.9F);
        poseStack.popPose();
    }
}
