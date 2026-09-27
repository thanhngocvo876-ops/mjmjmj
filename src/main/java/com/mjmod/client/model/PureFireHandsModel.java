package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

/** Lightweight Blockbench-style flame plates that wrap only each palm. */
public final class PureFireHandsModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_hands"), "main");

    private final ModelPart leftOuter;
    private final ModelPart leftInner;
    private final ModelPart leftAura;
    private final ModelPart rightOuter;
    private final ModelPart rightInner;
    private final ModelPart rightAura;
    private final ModelPart rightTrail;

    private static final float PUNCH_PREP_END = 3.0F / 14.0F;
    private static final float PUNCH_PUSH_END = 9.0F / 14.0F;
    private static final float PUNCH_HOLD_END = 11.0F / 14.0F;

    public PureFireHandsModel(ModelPart root) {
        leftOuter = root.getChild("left_outer");
        leftInner = root.getChild("left_inner");
        leftAura = root.getChild("left_aura");
        rightOuter = root.getChild("right_outer");
        rightInner = root.getChild("right_inner");
        rightAura = root.getChild("right_aura");
        rightTrail = root.getChild("right_trail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Two independently animated shells made from thin plates. They wrap
        // all four sides and the end of the fist while leaving the player's
        // hand visible through the gaps, like an armor render layer.
        addHand(root, "left_outer", 0, 0.34F);
        addHand(root, "left_inner", 8, 0.10F);
        addAura(root, "left_aura", 4);
        addHand(root, "right_outer", 16, 0.34F);
        addHand(root, "right_inner", 24, 0.10F);
        addAura(root, "right_aura", 12);
        root.addOrReplaceChild("right_trail", CubeListBuilder.create().texOffs(20, 0)
                .addBox(-1.2F, -3.1F, -6.2F, 2.4F, 2.6F, 5.8F,
                        new CubeDeformation(0.10F))
                .texOffs(20, 8)
                .addBox(-0.65F, -2.65F, -7.1F, 1.3F, 1.5F, 4.8F,
                        new CubeDeformation(0.06F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addHand(PartDefinition root, String name, int texV,
                                float shellOffset) {
        float side = 2.08F + shellOffset;
        float top = -2.15F - shellOffset;
        float bottom = 2.15F + shellOffset;
        float front = -2.15F - shellOffset;
        float back = 2.08F + shellOffset;
        CubeListBuilder plates = CubeListBuilder.create()
                // Front and back flame faces.
                .texOffs(0, texV)
                .addBox(-2.45F, -2.30F, front, 4.90F, 4.75F, 0.22F)
                .texOffs(0, texV)
                .addBox(-2.45F, -2.30F, back, 4.90F, 4.75F, 0.22F)
                // Side faces complete the fist wrap without creating a
                // thick opaque cube.
                .texOffs(8, texV)
                .addBox(-side - 0.22F, -2.30F, -2.05F,
                        0.22F, 4.75F, 4.10F)
                .texOffs(8, texV)
                .addBox(side, -2.30F, -2.05F,
                        0.22F, 4.75F, 4.10F)
                // A broken top/end cap keeps the silhouette fiery rather
                // than looking like a solid glove.
                .texOffs(16, texV)
                .addBox(-2.15F, top - 0.22F, -1.75F,
                        1.65F, 0.22F, 3.50F)
                .texOffs(16, texV)
                .addBox(0.45F, top - 0.22F, -1.75F,
                        1.70F, 0.22F, 3.50F)
                .texOffs(24, texV)
                .addBox(-1.65F, bottom, -1.60F,
                        3.30F, 0.22F, 3.20F)
                // Curled tongues rise from the wrist and finger-root edges.
                .texOffs(0, texV)
                .addBox(-2.15F, -3.75F, -1.75F,
                        0.80F, 1.70F, 0.24F)
                .texOffs(4, texV)
                .addBox(0.75F, -3.40F, 1.50F,
                        0.90F, 1.35F, 0.24F);
        root.addOrReplaceChild(name, plates, PartPose.ZERO);
    }

    /**
     * Large, broken flame sheets around the whole hand/forearm silhouette.
     * The red-circled reference is not a compact glove: it is a broad aura
     * with several tongues rising past the wrist.  Thin separated plates keep
     * the player's arm readable and avoid making the effect a solid cube.
     */
    private static void addAura(PartDefinition root, String name, int texV) {
        CubeListBuilder aura = CubeListBuilder.create()
                // Front/back outer sheets: broad enough to read at distance,
                // but only a thin layer in Z so the hand is not buried.
                .texOffs(0, texV)
                .addBox(-4.10F, -4.85F, -2.62F, 8.20F, 1.00F, 0.22F,
                        new CubeDeformation(0.12F))
                .texOffs(0, texV)
                .addBox(-4.45F, -4.05F, -2.58F, 1.05F, 6.55F, 0.22F,
                        new CubeDeformation(0.10F))
                .texOffs(8, texV)
                .addBox(3.40F, -4.55F, -2.56F, 1.05F, 7.05F, 0.22F,
                        new CubeDeformation(0.10F))
                .texOffs(16, texV)
                .addBox(-3.55F, 3.35F, -2.52F, 7.10F, 0.92F, 0.22F,
                        new CubeDeformation(0.10F))
                // A second depth layer gives the aura volume without a solid
                // block and remains visible while the player turns.
                .texOffs(0, texV + 8)
                .addBox(-3.75F, -4.25F, 2.36F, 7.50F, 0.75F, 0.22F,
                        new CubeDeformation(0.08F))
                // Rising tongues reproduce the upward/outward flame silhouette
                // from the reference image.
                .texOffs(20, texV)
                .addBox(-5.35F, -8.25F, -1.72F, 1.00F, 4.65F, 0.24F,
                        new CubeDeformation(0.12F))
                .texOffs(20, texV + 8)
                .addBox(4.22F, -9.15F, -1.55F, 1.02F, 5.55F, 0.24F,
                        new CubeDeformation(0.12F))
                .texOffs(24, texV)
                .addBox(-6.25F, -6.25F, -1.28F, 0.78F, 3.55F, 0.22F,
                        new CubeDeformation(0.08F))
                .texOffs(24, texV + 8)
                .addBox(5.35F, -6.05F, -1.18F, 0.78F, 3.90F, 0.22F,
                        new CubeDeformation(0.08F));
        root.addOrReplaceChild(name, aura, PartPose.ZERO);
    }

    public void animate(float age, float punchProgress, boolean handForm) {
        float flow = age * 0.28F;
        float pulse = 0.92F + Mth.sin(age * 0.55F) * 0.06F;
        // Four-phase, 0.7-second punch timeline: grow during preparation,
        // fill out during the push, hold at full size, then fade with the
        // returning fist.
        float punchGrowth;
        if (handForm) {
            punchGrowth = 1.0F;
        } else if (punchProgress < PUNCH_PREP_END) {
            punchGrowth = 0.55F + 0.30F * Mth.clamp(punchProgress / PUNCH_PREP_END, 0.0F, 1.0F);
        } else if (punchProgress < PUNCH_PUSH_END) {
            punchGrowth = 0.85F + 0.15F * Mth.clamp((punchProgress - PUNCH_PREP_END)
                    / (PUNCH_PUSH_END - PUNCH_PREP_END),
                    0.0F, 1.0F);
        } else if (punchProgress < PUNCH_HOLD_END) {
            punchGrowth = 1.0F;
        } else {
            punchGrowth = 1.0F - 0.55F * Mth.clamp((punchProgress - PUNCH_HOLD_END)
                    / (1.0F - PUNCH_HOLD_END), 0.0F, 1.0F);
        }
        float punch = punchProgress <= 0.0F ? 0.0F : 1.0F;
        animatePart(leftOuter, flow + 1.8F, pulse, 0.0F, 1.0F);
        animatePart(leftInner, flow + 3.0F, pulse * 0.92F, 0.0F, 1.0F);
        animateAura(leftAura, flow + 2.4F, pulse * (handForm ? 1.0F : 0.82F));
        animatePart(rightOuter, flow, pulse * punchGrowth, punch, punchGrowth);
        animatePart(rightInner, flow + 1.2F, pulse * 0.92F * punchGrowth,
                punch, punchGrowth);
        animateAura(rightAura, flow + 0.6F, pulse * (handForm ? 1.0F : 0.82F));
        float trail;
        if (punchProgress < PUNCH_PREP_END) trail = 0.0F;
        else if (punchProgress < PUNCH_PUSH_END) {
            trail = Mth.clamp((punchProgress - PUNCH_PREP_END)
                    / (PUNCH_PUSH_END - PUNCH_PREP_END), 0.0F, 1.0F);
        } else if (punchProgress < PUNCH_HOLD_END) trail = 1.0F;
        else trail = 1.0F - Mth.clamp((punchProgress - PUNCH_HOLD_END)
                / (1.0F - PUNCH_HOLD_END), 0.0F, 1.0F);
        rightTrail.xRot = Mth.sin(flow * 1.2F) * 0.06F;
        rightTrail.yRot = Mth.cos(flow * 0.9F) * 0.08F;
        rightTrail.zRot = Mth.sin(flow * 1.1F) * 0.12F;
        rightTrail.xScale = pulse * punchGrowth * (0.72F + trail * 0.22F);
        rightTrail.yScale = pulse * punchGrowth * (0.72F + trail * 0.28F);
        rightTrail.zScale = pulse * punchGrowth * (0.10F + trail * 0.90F);
    }

    private static void animatePart(ModelPart part, float time, float pulse,
                                    float punch, float growth) {
        part.xRot = Mth.sin(time * 1.3F) * 0.10F - punch * 0.06F;
        part.yRot = Mth.cos(time * 0.9F) * 0.12F;
        part.zRot = Mth.sin(time * 1.1F) * 0.16F;
        part.xScale = pulse;
        part.yScale = pulse + Mth.sin(time) * 0.035F;
        part.zScale = growth;
    }

    private static void animateAura(ModelPart part, float time, float pulse) {
        // Independent phase offsets make the large aura read as flowing flame
        // rather than a rigid shell attached to the arm.
        part.xRot = Mth.sin(time * 0.72F) * 0.10F;
        part.yRot = Mth.cos(time * 0.55F) * 0.14F;
        part.zRot = Mth.sin(time * 0.66F + 0.8F) * 0.13F;
        part.xScale = pulse * (1.0F + Mth.sin(time * 0.42F) * 0.05F);
        part.yScale = pulse * (1.0F + Mth.cos(time * 0.50F) * 0.07F);
        part.zScale = pulse * (0.96F + Mth.sin(time * 0.38F) * 0.04F);
    }

    public void renderLeft(PoseStack poseStack, VertexConsumer consumer, int light,
                           int overlay, float r, float g, float b, float alpha) {
        leftOuter.render(poseStack, consumer, light, overlay, r, g, b, alpha);
        leftInner.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderLeftAura(PoseStack poseStack, VertexConsumer consumer, int light,
                               int overlay, float r, float g, float b, float alpha) {
        leftAura.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderRight(PoseStack poseStack, VertexConsumer consumer, int light,
                            int overlay, float r, float g, float b, float alpha) {
        rightOuter.render(poseStack, consumer, light, overlay, r, g, b, alpha);
        rightInner.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderRightAura(PoseStack poseStack, VertexConsumer consumer, int light,
                                int overlay, float r, float g, float b, float alpha) {
        rightAura.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderPunchTrail(PoseStack poseStack, VertexConsumer consumer, int light,
                                 int overlay, float r, float g, float b, float alpha) {
        rightTrail.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }
}
