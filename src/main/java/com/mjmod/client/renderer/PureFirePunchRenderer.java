package com.mjmod.client.renderer;

import com.mjmod.entity.PureFirePunchEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;

/** The punch controller is invisible; the player hand layer draws the model. */
public final class PureFirePunchRenderer extends EntityRenderer<PureFirePunchEntity> {
    public PureFirePunchRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(PureFirePunchEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) { }

    @Override
    public boolean shouldRender(PureFirePunchEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFirePunchEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/unknown_server.png");
    }
}
