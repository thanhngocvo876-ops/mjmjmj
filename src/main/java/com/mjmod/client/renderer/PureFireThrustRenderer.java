package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireThrustEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PureFireThrustRenderer extends EntityRenderer<PureFireThrustEntity> {
    public PureFireThrustRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PureFireThrustEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireThrustEntity entity) {
        return null;
    }
}
