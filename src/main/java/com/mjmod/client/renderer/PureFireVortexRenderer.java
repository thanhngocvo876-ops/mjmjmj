package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.entity.PureFireVortexEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;

/** Entity controller renderer; the visible model is attached to the right arm bone. */
public class PureFireVortexRenderer extends EntityRenderer<PureFireVortexEntity> {
    private static final ResourceLocation FIRE_TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/effect/pure_fire_flame_32.png");

    public PureFireVortexRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override public boolean shouldRender(PureFireVortexEntity entity, Frustum frustum,
                                           double camX, double camY, double camZ) { return true; }

    @Override public void render(PureFireVortexEntity entity, float entityYaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // The visible effect is rendered by PureFireHandsLayer from the
        // player's right-arm bone. This entity renderer intentionally remains
        // empty so the effect cannot be drawn twice at two different anchors.
    }

    @Override public ResourceLocation getTextureLocation(PureFireVortexEntity entity) { return FIRE_TEXTURE; }
}
