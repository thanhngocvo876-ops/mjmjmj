package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireWallCastEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PureFireWallCastRenderer extends EntityRenderer<PureFireWallCastEntity> {
    public PureFireWallCastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PureFireWallCastEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireWallCastEntity entity) {
        return null;
    }
}
