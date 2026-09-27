package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.ShenQingShuiZhiYaoModel;
import com.mjmod.entity.ShenQingShuiZhiYaoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShenQingShuiZhiYaoRenderer
        extends MobRenderer<ShenQingShuiZhiYaoEntity, ShenQingShuiZhiYaoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/shuizhiyao.png");

    public ShenQingShuiZhiYaoRenderer(EntityRendererProvider.Context context) {
        super(context, new ShenQingShuiZhiYaoModel(
                context.bakeLayer(ShenQingShuiZhiYaoModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    protected void scale(ShenQingShuiZhiYaoEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(ShenQingShuiZhiYaoEntity entity) { return TEXTURE; }
}
