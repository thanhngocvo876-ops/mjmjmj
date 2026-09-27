package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * 独立的二技能长泪流模型。几何与一技能的长激光保持一致，但使用
 * 独立的 layer location，避免新增技能覆盖已有技能的模型层。
 */
public class VoidTearShotModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tear_shot"), "main");

    private final ModelPart outer;
    private final ModelPart core;

    public VoidTearShotModel(ModelPart root) {
        outer = root.getChild("tear");
        core = root.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Same authored dimensions as VoidTearBeamModel: a 20 px outer rail
        // with a 20.8 px bright core along local Z.
        root.addOrReplaceChild("tear", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.2F, -1.2F, -10.0F, 2.4F, 2.4F, 20.0F), PartPose.ZERO);
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-0.45F, -0.45F, -10.4F, 0.9F, 0.9F, 20.8F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = 1.0F + (float) Math.sin(ageInTicks * 0.42F) * 0.06F;
        outer.xScale = pulse;
        outer.yScale = pulse;
        outer.zScale = 1.0F + (float) Math.sin(ageInTicks * 0.37F) * 0.04F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        outer.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        core.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderOuter(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float r, float g, float b, float a) {
        outer.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderCore(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float r, float g, float b, float a) {
        core.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
