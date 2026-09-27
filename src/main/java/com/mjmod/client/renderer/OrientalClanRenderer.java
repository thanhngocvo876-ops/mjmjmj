package com.mjmod.client.renderer;

import com.mjmod.client.model.OrientalClanModel;
import com.mjmod.entity.OrientalClanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Steve appearance plus a visible diamond sword in the main hand. */
public class OrientalClanRenderer extends MobRenderer<OrientalClanEntity, OrientalClanModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft", "textures/entity/player/wide/steve.png");

    public OrientalClanRenderer(EntityRendererProvider.Context context) {
        super(context, new OrientalClanModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(OrientalClanEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(OrientalClanEntity entity) { return TEXTURE; }
}
