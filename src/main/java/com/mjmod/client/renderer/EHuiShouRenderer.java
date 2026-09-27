package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.client.model.EHuiShouModel;
import com.mjmod.entity.EHuiShouEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class EHuiShouRenderer extends MobRenderer<EHuiShouEntity, EHuiShouModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/ehuishou.png");

    public EHuiShouRenderer(EntityRendererProvider.Context context) {
        // The model is intentionally very large; keep the shadow close to its
        // visual footprint after the scale change below.
        super(context, new EHuiShouModel(context.bakeLayer(EHuiShouModel.LAYER_LOCATION)), 5.0F);
    }

    @Override
    protected void scale(EHuiShouEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(8.0F, 8.0F, 8.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EHuiShouEntity entity) {
        return TEXTURE;
    }
}
