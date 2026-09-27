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
import net.minecraft.world.entity.Entity;

/**
 * 动漫云团形的虚空之泪水盾。
 *
 * 盾面由相互重叠的薄水膜和上下左右的圆润水团组成，不生成封闭方块。
 * 每个水团在蓄力时从自己的底边向上长出，并持续产生液态波动。
 */
public class VoidTearShieldModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tear_shield"), "main");

    private final ModelPart core;
    private final ModelPart middleLeft;
    private final ModelPart middleRight;
    private final ModelPart upperLeft;
    private final ModelPart upperCenter;
    private final ModelPart upperRight;
    private final ModelPart lowerLeft;
    private final ModelPart lowerRight;

    public VoidTearShieldModel(ModelPart root) {
        core = root.getChild("core");
        middleLeft = root.getChild("middle_left");
        middleRight = root.getChild("middle_right");
        upperLeft = root.getChild("upper_left");
        upperCenter = root.getChild("upper_center");
        upperRight = root.getChild("upper_right");
        lowerLeft = root.getChild("lower_left");
        lowerRight = root.getChild("lower_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation softEdge = new CubeDeformation(0.18F);

        addBlob(root, "core", 22.0F, 52.0F, 2.4F, softEdge);
        addBlob(root, "middle_left", 22.0F, 30.0F, 2.2F, softEdge);
        addBlob(root, "middle_right", 22.0F, 30.0F, 2.2F, softEdge);
        addBlob(root, "upper_left", 18.0F, 23.0F, 2.0F, softEdge);
        addBlob(root, "upper_center", 18.0F, 25.0F, 2.1F, softEdge);
        addBlob(root, "upper_right", 18.0F, 23.0F, 2.0F, softEdge);
        addBlob(root, "lower_left", 20.0F, 21.0F, 2.1F, softEdge);
        addBlob(root, "lower_right", 20.0F, 21.0F, 2.1F, softEdge);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addBlob(PartDefinition root, String name,
                                float width, float height, float depth,
                                CubeDeformation deformation) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 0)
                .addBox(-width / 2.0F, -height / 2.0F, -depth / 2.0F,
                        width, height, depth, deformation), PartPose.ZERO);
    }

    public void animate(float time, float progress) {
        float p = clamp(progress);
        // Center first, then the side and top lobes, producing a compact cloud
        // silhouette rather than five separated columns.
        animateBlob(core, time, p, 0.0F, 0.0F, 0.0F, 26.0F, 0.0F, 0.00F);
        animateBlob(middleLeft, time, p, -17.0F, 1.0F, 0.4F, 15.0F, -0.6F, 0.08F);
        animateBlob(middleRight, time, p, 17.0F, 1.0F, -0.4F, 15.0F, 0.6F, 0.12F);
        animateBlob(upperLeft, time, p, -13.0F, -21.0F, -0.5F, 11.5F, -0.9F, 0.24F);
        animateBlob(upperCenter, time, p, 0.0F, -25.0F, 0.0F, 12.5F, 0.0F, 0.34F);
        animateBlob(upperRight, time, p, 13.0F, -21.0F, 0.5F, 11.5F, 0.9F, 0.28F);
        animateBlob(lowerLeft, time, p, -13.0F, 21.0F, 0.6F, 8.0F, -0.8F, 0.42F);
        animateBlob(lowerRight, time, p, 13.0F, 21.0F, -0.6F, 8.0F, 0.8F, 0.48F);
    }

    private static void animateBlob(ModelPart part, float time, float progress,
                                     float baseX, float baseY, float baseZ,
                                     float halfHeight, float roll, float delay) {
        float local = clamp((progress - delay) / (1.0F - delay));
        float wave = (float) Math.sin(time * 0.24F + baseX * 0.08F);
        float slow = (float) Math.sin(time * 0.14F + baseY * 0.06F);
        float scale = 0.05F + local * (0.95F + wave * 0.04F);
        // The lower edge stays in place while the lobe grows upward.
        part.x = baseX + slow * 0.8F;
        part.y = baseY + halfHeight * (1.0F - local);
        part.z = baseZ + wave * 1.5F;
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = 0.92F + slow * 0.06F;
        part.yRot = wave * 0.12F;
        part.zRot = roll + slow * 0.08F;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        core.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        middleLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        middleRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        upperLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        upperCenter.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        upperRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        lowerLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        lowerRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
