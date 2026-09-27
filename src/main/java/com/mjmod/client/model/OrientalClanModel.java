package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mjmod.entity.OrientalClanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

/** Standard Steve model used temporarily for the wandering Oriental clan. */
public class OrientalClanModel extends HumanoidModel<OrientalClanEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "oriental_clan"), "main");

    public OrientalClanModel(ModelPart root) { super(root); }
}
