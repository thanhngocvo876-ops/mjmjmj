package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Low-poly Blockbench-style voxel flame made from layered cubes. */
public class PureFireVortexModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_vortex"), "main");
    private final ModelPart outer;
    private final ModelPart inner;
    private final ModelPart crest;
    private final ModelPart wrapLeft;
    private final ModelPart wrapRight;
    private final ModelPart wrapTop;
    private final ModelPart wrapBottom;
    private final ModelPart tongueLeft;
    private final ModelPart tongueRight;
    private final ModelPart tongueTop;
    private final ModelPart tongueBottom;
    private final ModelPart rootBurst;

    public PureFireVortexModel(ModelPart root) {
        outer = root.getChild("outer");
        inner = root.getChild("inner");
        crest = root.getChild("crest");
        wrapLeft = root.getChild("wrap_left");
        wrapRight = root.getChild("wrap_right");
        wrapTop = root.getChild("wrap_top");
        wrapBottom = root.getChild("wrap_bottom");
        tongueLeft = root.getChild("tongue_left");
        tongueRight = root.getChild("tongue_right");
        tongueTop = root.getChild("tongue_top");
        tongueBottom = root.getChild("tongue_bottom");
        rootBurst = root.getChild("root_burst");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The model is authored along local Z and stretched by the renderer.
        root.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.65F, -2.65F, -4.0F, 5.3F, 5.3F, 2.35F,
                        new CubeDeformation(0.32F))
                .texOffs(0, 0)
                .addBox(-2.35F, -2.35F, -1.85F, 4.7F, 4.7F, 2.65F,
                        new CubeDeformation(0.28F))
                .texOffs(0, 0)
                .addBox(-2.02F, -2.02F, 0.70F, 4.04F, 4.04F, 2.45F,
                        new CubeDeformation(0.24F))
                .texOffs(0, 0)
                .addBox(-1.58F, -1.58F, 3.02F, 3.16F, 3.16F, 0.98F,
                        new CubeDeformation(0.35F)), PartPose.ZERO);
        root.addOrReplaceChild("inner", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-1.45F, -1.45F, -4.2F, 2.9F, 2.9F, 2.55F,
                        new CubeDeformation(0.18F))
                .texOffs(0, 12)
                .addBox(-1.20F, -1.20F, -1.95F, 2.4F, 2.4F, 2.80F,
                        new CubeDeformation(0.14F))
                .texOffs(0, 12)
                .addBox(-0.92F, -0.92F, 0.70F, 1.84F, 1.84F, 2.65F,
                        new CubeDeformation(0.10F))
                .texOffs(0, 12)
                .addBox(-0.58F, -0.58F, 3.05F, 1.16F, 1.16F, 0.88F,
                        new CubeDeformation(0.08F)), PartPose.ZERO);
        root.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(16, 12)
                .addBox(-1.0F, -3.1F, -3.2F, 2.0F, 1.2F, 6.4F,
                        new CubeDeformation(0.12F)), PartPose.ZERO);
        // Separate 3-D tongues which rotate around the core; these are
        // rendered geometry, not particles.
        root.addOrReplaceChild("wrap_left", CubeListBuilder.create().texOffs(0, 20)
                .addBox(-3.55F, -0.55F, -3.4F, 1.15F, 1.1F, 6.8F,
                        new CubeDeformation(0.16F)), PartPose.ZERO);
        root.addOrReplaceChild("wrap_right", CubeListBuilder.create().texOffs(8, 20)
                .addBox(2.40F, -0.55F, -3.4F, 1.15F, 1.1F, 6.8F,
                        new CubeDeformation(0.16F)), PartPose.ZERO);
        root.addOrReplaceChild("wrap_top", CubeListBuilder.create().texOffs(16, 20)
                .addBox(-0.55F, -3.55F, -3.4F, 1.1F, 1.15F, 6.8F,
                        new CubeDeformation(0.16F)), PartPose.ZERO);
        root.addOrReplaceChild("wrap_bottom", CubeListBuilder.create().texOffs(24, 20)
                .addBox(-0.55F, 2.40F, -3.4F, 1.1F, 1.15F, 6.8F,
                        new CubeDeformation(0.16F)), PartPose.ZERO);
        root.addOrReplaceChild("tongue_left", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-4.10F, -1.35F, -3.55F, 0.82F, 2.70F, 2.20F,
                        new CubeDeformation(0.18F))
                .texOffs(0, 24)
                .addBox(-4.85F, -0.95F, -1.65F, 0.68F, 1.90F, 2.50F,
                        new CubeDeformation(0.14F)), PartPose.ZERO);
        root.addOrReplaceChild("tongue_right", CubeListBuilder.create().texOffs(8, 24)
                .addBox(3.28F, -1.25F, -3.05F, 0.82F, 2.50F, 2.50F,
                        new CubeDeformation(0.18F))
                .texOffs(8, 24)
                .addBox(4.22F, -0.90F, -0.90F, 0.68F, 1.80F, 2.65F,
                        new CubeDeformation(0.14F)), PartPose.ZERO);
        root.addOrReplaceChild("tongue_top", CubeListBuilder.create().texOffs(16, 24)
                .addBox(-1.30F, -4.10F, -2.80F, 2.60F, 0.78F, 2.25F,
                        new CubeDeformation(0.18F))
                .texOffs(16, 24)
                .addBox(-0.90F, -4.85F, -0.85F, 1.80F, 0.62F, 2.10F,
                        new CubeDeformation(0.14F)), PartPose.ZERO);
        root.addOrReplaceChild("tongue_bottom", CubeListBuilder.create().texOffs(24, 24)
                .addBox(-1.22F, 3.28F, -2.60F, 2.44F, 0.78F, 2.35F,
                        new CubeDeformation(0.18F))
                .texOffs(24, 24)
                .addBox(-0.82F, 4.18F, -0.55F, 1.64F, 0.62F, 1.90F,
                        new CubeDeformation(0.14F)), PartPose.ZERO);
        root.addOrReplaceChild("root_burst", CubeListBuilder.create().texOffs(0, 28)
                .addBox(-3.35F, -3.35F, -4.05F, 6.70F, 0.58F, 1.05F,
                        new CubeDeformation(0.12F))
                .texOffs(14, 28)
                .addBox(-0.58F, -3.80F, -4.10F, 1.16F, 7.60F, 1.16F,
                        new CubeDeformation(0.10F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void animate(float time, float charge) {
        float flow = time * 0.28F;
        float wave = Mth.sin(time * 0.42F);
        outer.yRot = wave * 0.12F;
        outer.xRot = Mth.cos(time * 0.31F) * 0.08F;
        inner.yRot = -wave * 0.18F;
        inner.zRot = Mth.sin(time * 0.55F) * 0.08F;
        crest.yRot = wave * 0.23F;
        crest.x = Mth.sin(time * 0.38F) * 0.5F;
        float spin = time * (0.14F + charge * 0.16F);
        float wrapPulse = 0.82F + Mth.sin(time * 0.75F) * 0.10F + charge * 0.10F;
        animateWrap(wrapLeft, spin, -1.0F, wrapPulse);
        animateWrap(wrapRight, spin + Mth.PI, 1.0F, wrapPulse);
        animateWrap(wrapTop, spin + Mth.PI * 0.5F, 0.72F, wrapPulse);
        animateWrap(wrapBottom, spin + Mth.PI * 1.5F, -0.72F, wrapPulse);
        animateTongue(tongueLeft, flow + 0.4F, -1.0F, wrapPulse);
        animateTongue(tongueRight, flow + 2.1F, 1.0F, wrapPulse);
        animateTongue(tongueTop, flow + 3.4F, 0.65F, wrapPulse);
        animateTongue(tongueBottom, flow + 4.8F, -0.65F, wrapPulse);
        rootBurst.xRot = Mth.sin(flow * 0.75F) * 0.08F;
        rootBurst.yRot = Mth.cos(flow * 0.62F) * 0.12F;
        rootBurst.zRot = Mth.sin(flow * 0.85F) * 0.16F;
        rootBurst.xScale = wrapPulse * 1.04F;
        rootBurst.yScale = wrapPulse * 1.04F;
        float pulse = 0.92F + Mth.sin(time * 0.8F) * 0.05F + charge * 0.05F;
        outer.xScale = pulse;
        outer.yScale = pulse;
        inner.xScale = 0.98F + charge * 0.04F;
        inner.yScale = 0.98F + charge * 0.04F;
    }

    private static void animateWrap(ModelPart part, float spin, float side, float pulse) {
        part.yRot = Mth.sin(spin) * 0.22F;
        part.xRot = Mth.cos(spin * 0.87F) * 0.16F;
        part.zRot = Mth.sin(spin * 1.13F) * 0.28F;
        part.x = side * (2.55F + Mth.sin(spin) * 0.42F);
        part.y = Mth.cos(spin) * 0.42F;
        part.xScale = pulse;
        part.yScale = pulse;
    }

    private static void animateTongue(ModelPart part, float time, float side, float pulse) {
        part.xRot = Mth.cos(time * 0.78F) * 0.22F;
        part.yRot = Mth.sin(time * 0.64F) * 0.24F;
        part.zRot = Mth.sin(time * 0.92F) * 0.34F;
        part.x = side * (0.10F + Mth.sin(time * 0.48F) * 0.28F);
        part.y = Mth.cos(time * 0.57F) * 0.24F;
        part.xScale = pulse * (0.92F + Mth.sin(time * 0.41F) * 0.08F);
        part.yScale = pulse * (0.94F + Mth.cos(time * 0.46F) * 0.08F);
        part.zScale = pulse;
    }

    @Override public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {}

    @Override public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                         int packedOverlay, float r, float g, float b, float a) {
        outer.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        inner.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        crest.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        wrapLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        wrapRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        wrapTop.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        wrapBottom.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        tongueLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        tongueRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        tongueTop.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        tongueBottom.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        rootBurst.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderOuter(PoseStack p, VertexConsumer b, int l, int o, float r, float g, float bl, float a) {
        outer.render(p, b, l, o, r, g, bl, a);
        crest.render(p, b, l, o, r, g, bl, a);
        wrapLeft.render(p, b, l, o, r, g, bl, a);
        wrapRight.render(p, b, l, o, r, g, bl, a);
        wrapTop.render(p, b, l, o, r, g, bl, a);
        wrapBottom.render(p, b, l, o, r, g, bl, a);
        tongueLeft.render(p, b, l, o, r, g, bl, a);
        tongueRight.render(p, b, l, o, r, g, bl, a);
        tongueTop.render(p, b, l, o, r, g, bl, a);
        tongueBottom.render(p, b, l, o, r, g, bl, a);
        rootBurst.render(p, b, l, o, r, g, bl, a);
    }

    public void renderInner(PoseStack p, VertexConsumer b, int l, int o, float r, float g, float bl, float a) {
        inner.render(p, b, l, o, r, g, bl, a);
    }
}
