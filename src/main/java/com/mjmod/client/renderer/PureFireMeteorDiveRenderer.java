package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireMeteorDiveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Invisible controller; the player renderer owns the authored body animation. */
public final class PureFireMeteorDiveRenderer extends EntityRenderer<PureFireMeteorDiveEntity> {
    public PureFireMeteorDiveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PureFireMeteorDiveEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) { }

    @Override
    public boolean shouldRender(PureFireMeteorDiveEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireMeteorDiveEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/unknown_server.png");
    }
}
