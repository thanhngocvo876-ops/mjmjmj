package com.mjmod.client.renderer;

import com.mjmod.entity.PureFireBallCastEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * 火球施法实体本身不可见，其动作由客户端 Player Animator 驱动玩家模型展示。
 */
public final class PureFireBallCastRenderer extends EntityRenderer<PureFireBallCastEntity> {
    public PureFireBallCastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PureFireBallCastEntity entity, Frustum frustum, double x, double y, double z) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireBallCastEntity entity) {
        return null;
    }
}
