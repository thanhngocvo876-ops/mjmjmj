package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Blockbench 风格的弧月水刃。
 *
 * <p>泪刃不是一根长方条，而是由有厚度的整块水刃沿外弧拼接而成。
 * 内缘的四块水波和少量脱落水块让轮廓保持不规则，同时仍能在
 * Blockbench 中逐块编辑。</p>
 */
public class VoidTearBladeModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tear_blade"), "main");

    private static final float[] ARC_X = {-15.0F, -3.0F, 10.0F, 18.0F, 20.0F,
            18.0F, 10.0F, -3.0F, -15.0F};
    private static final float[] ARC_Y = {-24.0F, -29.0F, -24.0F, -14.0F, 0.0F,
            14.0F, 24.0F, 29.0F, 24.0F};
    private static final float[] ARC_WIDTH = {16.0F, 18.0F, 17.0F, 15.0F, 14.0F,
            15.0F, 17.0F, 18.0F, 16.0F};
    private static final float[] ARC_HEIGHT = {8.0F, 10.0F, 13.0F, 16.0F, 20.0F,
            16.0F, 13.0F, 10.0F, 8.0F};
    private static final float[] ARC_ROTATION = {-0.35F, -0.10F, 0.52F, 1.02F, 1.57F,
            -1.02F, -0.52F, 0.10F, 0.35F};

    private static final float[] WAVE_X = {2.0F, 5.0F, 6.0F, 3.0F};
    private static final float[] WAVE_Y = {-16.0F, -6.0F, 6.0F, 16.0F};
    private static final float[] WAVE_WIDTH = {9.0F, 11.0F, 10.0F, 9.0F};
    private static final float[] WAVE_ROTATION = {-0.75F, -0.35F, 0.35F, 0.75F};

    private static final float[] SPLASH_X = {-19.0F, -20.0F, 12.0F, 12.0F};
    private static final float[] SPLASH_Y = {-18.0F, 8.0F, -29.0F, 28.0F};
    private static final float[] SPLASH_ROTATION = {-0.55F, 0.42F, 0.26F, -0.30F};

    private final ModelPart[] arcSegments = new ModelPart[ARC_X.length];
    private final ModelPart[] innerWaves = new ModelPart[WAVE_X.length];
    private final ModelPart[] splashes = new ModelPart[SPLASH_X.length];
    private final ModelPart[] coreSegments = new ModelPart[ARC_X.length];

    public VoidTearBladeModel(ModelPart root) {
        for (int i = 0; i < arcSegments.length; i++) {
            arcSegments[i] = root.getChild("arc_" + i);
            coreSegments[i] = root.getChild("core_" + i);
        }
        for (int i = 0; i < innerWaves.length; i++) {
            innerWaves[i] = root.getChild("wave_" + i);
            splashes[i] = root.getChild("splash_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Nine overlapping chunks trace a thick crescent. The middle chunks
        // are tallest, while the ends taper into water-drop points.
        for (int i = 0; i < ARC_X.length; i++) {
            root.addOrReplaceChild("arc_" + i,
                    CubeListBuilder.create().texOffs((i * 4) % 28, 0).addBox(
                            -ARC_WIDTH[i] * 0.5F, -ARC_HEIGHT[i] * 0.5F, -2.5F,
                            ARC_WIDTH[i], ARC_HEIGHT[i], 5.0F),
                    PartPose.offsetAndRotation(ARC_X[i], ARC_Y[i], 0.0F,
                            0.0F, 0.0F, ARC_ROTATION[i]));

            float coreWidth = ARC_WIDTH[i] * 0.62F;
            float coreHeight = Math.max(4.0F, ARC_HEIGHT[i] * 0.54F);
            root.addOrReplaceChild("core_" + i,
                    CubeListBuilder.create().texOffs((i * 4 + 2) % 28, 16).addBox(
                            -coreWidth * 0.5F, -coreHeight * 0.5F, -2.9F,
                            coreWidth, coreHeight, 5.8F),
                    PartPose.offsetAndRotation(ARC_X[i] + 1.0F, ARC_Y[i], 0.0F,
                            0.0F, 0.0F, ARC_ROTATION[i]));
        }

        // These chunky inner pieces break up the concave edge like splashing
        // water instead of making it a mathematically perfect ring.
        for (int i = 0; i < WAVE_X.length; i++) {
            root.addOrReplaceChild("wave_" + i,
                    CubeListBuilder.create().texOffs((i * 6) % 28, 8).addBox(
                            -WAVE_WIDTH[i] * 0.5F, -2.5F, -2.0F,
                            WAVE_WIDTH[i], 5.0F, 4.0F),
                    PartPose.offsetAndRotation(WAVE_X[i], WAVE_Y[i], 0.0F,
                            0.0F, 0.0F, WAVE_ROTATION[i]));
            root.addOrReplaceChild("splash_" + i,
                    CubeListBuilder.create().texOffs((i * 5 + 12) % 28, 8).addBox(
                            -3.0F, -2.5F, -1.5F, 6.0F, 5.0F, 3.0F),
                    PartPose.offsetAndRotation(SPLASH_X[i], SPLASH_Y[i], 0.0F,
                            0.0F, 0.0F, SPLASH_ROTATION[i]));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float wave = Mth.sin(ageInTicks * 0.34F);
        for (int i = 0; i < arcSegments.length; i++) {
            float phase = ageInTicks * 0.21F + i * 0.52F;
            arcSegments[i].x = ARC_X[i] + Mth.sin(phase) * 0.45F;
            arcSegments[i].y = ARC_Y[i] + Mth.cos(phase * 0.91F) * 0.35F;
            arcSegments[i].zRot = ARC_ROTATION[i] + wave * (0.018F + i * 0.0015F);
            coreSegments[i].x = ARC_X[i] + 1.0F + Mth.sin(phase + 0.4F) * 0.30F;
            coreSegments[i].y = ARC_Y[i] + Mth.cos(phase * 0.91F + 0.4F) * 0.24F;
            coreSegments[i].zRot = ARC_ROTATION[i] - wave * 0.012F;
        }
        for (int i = 0; i < innerWaves.length; i++) {
            float phase = ageInTicks * 0.29F + i * 0.9F;
            innerWaves[i].x = WAVE_X[i] + Mth.sin(phase) * 0.55F;
            innerWaves[i].y = WAVE_Y[i] + Mth.cos(phase * 0.8F) * 0.45F;
            innerWaves[i].zRot = WAVE_ROTATION[i] + Mth.sin(phase) * 0.045F;
            splashes[i].x = SPLASH_X[i] + Mth.cos(phase * 0.75F) * 0.65F;
            splashes[i].y = SPLASH_Y[i] + Mth.sin(phase * 0.68F) * 0.55F;
            splashes[i].zRot = SPLASH_ROTATION[i] - Mth.cos(phase) * 0.06F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        renderOuter(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        renderCore(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }

    public void renderOuter(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float r, float g, float b, float a) {
        for (ModelPart segment : arcSegments) {
            segment.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
        for (ModelPart wave : innerWaves) {
            wave.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
        for (ModelPart splash : splashes) {
            splash.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
    }

    public void renderCore(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                           int packedOverlay, float r, float g, float b, float a) {
        for (ModelPart segment : coreSegments) {
            segment.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        }
    }
}
