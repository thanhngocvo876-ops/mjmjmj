package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireWallEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 纯质阳炎八技能【火墙】客户端渲染器：
 * 粒子逻辑已完全内聚在 PureFireWallEntity 的客户端 tick 中，
 * 避免了使用破损的多边形/三角形带导致的模型撕裂问题。
 */
public final class PureFireWallRenderer extends EntityRenderer<PureFireWallEntity> {
    public PureFireWallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PureFireWallEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireWallEntity entity) {
        return null;
    }
}
