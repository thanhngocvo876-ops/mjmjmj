package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

/**
 * The user's 阳炎拳 Blockbench model converted to two fist-rooted ModelParts.
 * The original 43 cubes are split at the Blockbench x=0 body plane and
 * normalized around the left/right fist centers before arm attachment.
 */
public final class PureFireFistModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_fist"), "main");

    private final ModelPart leftFist;
    private final ModelPart rightFist;
    private final ModelPart[] leftFlames;
    private final ModelPart[] rightFlames;

    public PureFireFistModel(ModelPart root) {
        leftFist = root.getChild("left_fist");
        rightFist = root.getChild("right_fist");
        leftFlames = cubeParts(leftFist, 23);
        rightFlames = cubeParts(rightFist, 22);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addFist(root, "left_fist", LEFT_BOXES);
        addFist(root, "right_fist", RIGHT_BOXES);
        // Blockbench stores a 64x64 image but this model's UV coordinate
        // space is explicitly 32x32 (uv_width/uv_height in the .bbmodel).
        // Keep the model grid at 32 so every face samples the authored pixels.
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addFist(PartDefinition root, String name, Box[] boxes) {
        PartDefinition fist = root.addOrReplaceChild(name,
                CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < boxes.length; i++) {
            Box box = boxes[i];
            fist.addOrReplaceChild("cube_" + i,
                    CubeListBuilder.create().texOffs(box.u, box.v)
                            .addBox(box.x, box.y, box.z, box.dx, box.dy, box.dz,
                                    new CubeDeformation(0.0F)),
                    PartPose.ZERO);
        }
    }

    private static ModelPart[] cubeParts(ModelPart fist, int count) {
        ModelPart[] parts = new ModelPart[count];
        for (int i = 0; i < count; i++) {
            parts[i] = fist.getChild("cube_" + i);
        }
        return parts;
    }

    /**
     * Visible flowing-flame animation inside the Blockbench model. The arm
     * bone remains untouched: each authored fire cube gets a different wave
     * phase, lift, sway and pulse so the glove visibly ripples instead of only
     * receiving an imperceptible whole-model wobble.
     */
    public void animate(float ageInTicks) {
        animateFist(leftFist, leftFlames, ageInTicks * 0.19F, true);
        animateFist(rightFist, rightFlames, ageInTicks * 0.19F, false);
    }

    /** Plays the uploaded 直冲 clip: charge in, hold the last frame, retract. */
    public void animatePunch(float clip, int phase, float ageInTicks) {
        animatePunchFist(leftFist, leftFlames, clip, phase, ageInTicks, true);
        animatePunchFist(rightFist, rightFlames, clip, phase, ageInTicks, false);
    }

    private static void animatePunchFist(ModelPart fist, ModelPart[] flames,
                                         float clip, int phase, float ageInTicks,
                                         boolean left) {
        fist.resetPose();
        for (ModelPart flame : flames) flame.resetPose();
        // The arm rotations are supplied by the matching Player Animator
        // keyframes. Only the authored flame pieces receive a restrained
        // material-flow ripple, and phase 1 freezes it with the held pose.
        float time = phase == 1 ? 1.0F : clip;
        float side = left ? -1.0F : 1.0F;
        for (int i = 0; i < flames.length; i++) {
            ModelPart flame = flames[i];
            float phaseOffset = time * 2.0F + i * 0.24F + (left ? 0.0F : 0.15F);
            float wave = Mth.sin(phaseOffset);
            float edge = i / (float) Math.max(1, flames.length - 1);
            flame.x += side * wave * (0.018F + edge * 0.025F);
            flame.y += Mth.cos(phaseOffset * 0.80F) * (0.022F + edge * 0.028F);
            flame.z += Mth.sin(phaseOffset * 0.65F) * 0.018F;
            flame.zRot = wave * 0.018F;
            float pulse = 1.0F + Mth.sin(phaseOffset * 1.12F) * 0.014F;
            flame.xScale = pulse;
            flame.yScale = 1.0F + wave * 0.018F;
            flame.zScale = pulse;
        }
    }

    private static void animateFist(ModelPart fist, ModelPart[] flames,
                                    float time, boolean left) {
        fist.resetPose();
        float pulse = 1.0F + Mth.sin(time * 1.25F) * 0.022F;
        fist.xRot = Mth.sin(time * 0.80F) * 0.020F;
        fist.yRot = Mth.cos(time * 0.67F) * 0.025F;
        fist.zRot = Mth.sin(time * 0.73F) * 0.032F;
        fist.xScale = pulse;
        fist.yScale = pulse;
        fist.zScale = pulse;
        for (int i = 0; i < flames.length; i++) {
            ModelPart flame = flames[i];
            flame.resetPose();
            float phase = time * 2.20F + i * 0.63F + (left ? 0.0F : Mth.PI);
            float wave = Mth.sin(phase);
            float wave2 = Mth.cos(phase * 0.73F);
            float edge = i / (float) Math.max(1, flames.length - 1);
            flame.x += wave * (0.055F + edge * 0.060F);
            flame.y += wave2 * (0.065F + edge * 0.075F);
            flame.z += Mth.sin(phase * 0.61F) * 0.045F;
            flame.xRot = wave * 0.022F;
            flame.yRot = wave2 * 0.028F;
            flame.zRot = Mth.sin(phase * 0.82F) * 0.040F;
            float flamePulse = 1.0F + Mth.sin(phase * 1.17F) * 0.028F;
            flame.xScale = flamePulse;
            flame.yScale = 1.0F + wave * 0.040F;
            flame.zScale = flamePulse;
        }
    }

    public void renderLeft(PoseStack poseStack, VertexConsumer consumer, int light,
                           int overlay, float r, float g, float b, float alpha) {
        leftFist.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderRight(PoseStack poseStack, VertexConsumer consumer, int light,
                            int overlay, float r, float g, float b, float alpha) {
        rightFist.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    private record Box(float x, float y, float z, float dx, float dy, float dz,
                       int u, int v) {}

    private static final Box[] LEFT_BOXES = {
        new Box(-4F, 2F, -3F, 4F, 2F, 1F, 14, 6),
        new Box(-4F, 2F, 2F, 4F, 2F, 1F, 14, 9),
        new Box(0F, 2F, -3F, 1F, 2F, 6F, 6, 6),
        new Box(1F, -1F, -1F, 1F, 3F, 1F, 8, 22),
        new Box(3F, -4F, -1F, 1F, 3F, 1F, 16, 23),
        new Box(0F, 0F, -1F, 1F, 2F, 1F, 24, 24),
        new Box(2F, -3F, -1F, 1F, 4F, 1F, 18, 12),
        new Box(-4F, 0F, -3F, 1F, 2F, 1F, 26, 0),
        new Box(-4F, -1F, -1F, 1F, 3F, 1F, 22, 12),
        new Box(-3F, 0F, -3F, 1F, 1F, 1F, 28, 8),
        new Box(-2F, -3F, -3F, 1F, 5F, 1F, 14, 12),
        new Box(-1F, 1F, 2F, 1F, 1F, 1F, 12, 28),
        new Box(-4F, 0F, 2F, 1F, 2F, 1F, 26, 3),
        new Box(-3F, -1F, 2F, 1F, 2F, 1F, 8, 26),
        new Box(-2F, -3F, 2F, 1F, 5F, 1F, 0, 16),
        new Box(0F, -2F, 2F, 1F, 4F, 1F, 16, 18),
        new Box(1F, 0F, 1F, 1F, 2F, 1F, 26, 10),
        new Box(0F, 1F, 1F, 1F, 1F, 1F, 28, 16),
        new Box(2F, -2F, 1F, 1F, 3F, 1F, 20, 22),
        new Box(0F, 1F, -3F, 1F, 1F, 1F, 28, 18),
        new Box(0F, 0F, -3F, 1F, 2F, 1F, 26, 13),
        new Box(2F, -1F, -3F, 1F, 2F, 1F, 20, 26),
        // Uploaded model封底, from (4,11,-3) to (9,12,3).
        new Box(-4F, 1F, -3F, 5F, 1F, 6F, 0, 0)
    };

    private static final Box[] RIGHT_BOXES = {
        new Box(-1F, 2F, -3F, 5F, 2F, 1F, 14, 0),
        new Box(0F, 2F, 2F, 4F, 2F, 1F, 14, 3),
        new Box(-1F, 2F, -3F, 1F, 2F, 6F, 0, 8),
        new Box(0F, -2F, -3F, 1F, 2F, 1F, 0, 27),
        new Box(-1F, -1F, -3F, 1F, 3F, 1F, 24, 6),
        new Box(2F, 0F, -3F, 1F, 1F, 1F, 28, 20),
        new Box(1F, -3F, -3F, 1F, 5F, 1F, 4, 16),
        new Box(3F, 1F, -3F, 1F, 1F, 1F, 28, 22),
        new Box(-1F, 1F, 2F, 1F, 1F, 1F, 28, 24),
        new Box(-1F, -2F, 2F, 1F, 4F, 1F, 20, 17),
        new Box(2F, -1F, 2F, 1F, 2F, 1F, 4, 27),
        new Box(1F, -3F, 2F, 1F, 5F, 1F, 8, 16),
        new Box(3F, 0F, 2F, 1F, 2F, 1F, 16, 27),
        new Box(-2F, -1F, 1F, 1F, 3F, 1F, 12, 24),
        new Box(-1F, 1F, 1F, 1F, 1F, 1F, 28, 26),
        new Box(-3F, -3F, 1F, 1F, 4F, 1F, 0, 22),
        new Box(-2F, -1F, -1F, 1F, 3F, 1F, 24, 16),
        new Box(-1F, 0F, -1F, 1F, 2F, 1F, 24, 27),
        new Box(-3F, -4F, -1F, 1F, 5F, 1F, 12, 18),
        new Box(-2F, -1F, -3F, 1F, 3F, 1F, 24, 20),
        new Box(-3F, -3F, -3F, 1F, 4F, 1F, 4, 22),
        // Uploaded model封底, from (-9,11,-3) to (-4,12,3).
        new Box(-1F, 1F, -3F, 5F, 1F, 6F, 0, 0)
    };
}
