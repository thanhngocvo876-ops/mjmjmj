package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireCleaveEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PureFireCleaveRenderer extends EntityRenderer<PureFireCleaveEntity> {
    public PureFireCleaveRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PureFireCleaveEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireCleaveEntity entity) {
        return null;
    }
}
