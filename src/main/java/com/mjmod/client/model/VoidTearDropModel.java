package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** 略大于原版的圆润白包蓝虚空水珠。 */
public class VoidTearDropModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tear_drop"), "main");

    private final ModelPart sphere;
    private final ModelPart ringHorizontal;
    private final ModelPart ringVertical;
    private final ModelPart highlight;
    private final ModelPart core;

    public VoidTearDropModel(ModelPart root) {
        sphere = root.getChild("sphere");
        ringHorizontal = root.getChild("ring_horizontal");
        ringVertical = root.getChild("ring_vertical");
        highlight = root.getChild("highlight");
        core = root.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // A 5-pixel rounded cube plus two crossing water bands reads as a
        // spherical droplet in Minecraft's pixel geometry, with no pointed tip.
        root.addOrReplaceChild("sphere", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F,
                        new CubeDeformation(0.55F)), PartPose.ZERO);
        root.addOrReplaceChild("ring_horizontal", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-3.0F, -0.65F, -2.35F, 6.0F, 1.3F, 4.7F,
                        new CubeDeformation(0.25F)), PartPose.ZERO);
        root.addOrReplaceChild("ring_vertical", CubeListBuilder.create().texOffs(12, 12)
                .addBox(-0.65F, -2.35F, -3.0F, 1.3F, 4.7F, 6.0F,
                        new CubeDeformation(0.25F)), PartPose.ZERO);
        root.addOrReplaceChild("highlight", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-1.55F, -1.75F, -3.0F, 1.25F, 1.35F, 0.45F,
                        new CubeDeformation(0.08F)), PartPose.ZERO);
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(20, 12)
                .addBox(-1.55F, -1.55F, -2.85F, 3.1F, 3.1F, 1.15F,
                        new CubeDeformation(0.20F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float wave = Mth.sin(ageInTicks * 0.28F);
        sphere.xScale = 1.0F + wave * 0.035F;
        sphere.yScale = 1.0F - wave * 0.025F;
        sphere.zScale = 1.0F + wave * 0.025F;
        ringHorizontal.yRot = wave * 0.045F;
        ringVertical.xRot = -wave * 0.045F;
        highlight.x = -1.55F + Mth.sin(ageInTicks * 0.20F) * 0.06F;
        core.zRot = Mth.sin(ageInTicks * 0.24F) * 0.025F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        renderOuter(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        renderCore(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderOuter(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float r, float g, float b, float a) {
        sphere.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        ringHorizontal.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        ringVertical.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        highlight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderCore(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float r, float g, float b, float a) {
        core.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
