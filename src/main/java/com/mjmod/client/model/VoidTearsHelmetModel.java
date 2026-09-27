package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 贴合玩家头部的虚空之泪：右眼液态焰、额头尖焰和向左上抽拉的弧形尾焰。
 * Blockbench 工程中的像素坐标按 1/16 比例直接复刻到这里。
 */
public class VoidTearsHelmetModel extends HumanoidModel<Player> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tears_helmet"), "main");

    private final ModelPart tearEyeCore;
    private final ModelPart tearEyeLobe;
    private final ModelPart tearCheekDrop;
    private final ModelPart tearBrow;
    private final ModelPart crownLeft;
    private final ModelPart crownCenter;
    private final ModelPart crownRight;
    private final ModelPart tailFront;
    private final ModelPart tailMid;
    private final ModelPart tailBack;
    private final ModelPart tailTip;
    private final ModelPart dropOuter;
    private final ModelPart dropLower;
    private final ModelPart dropCrown;

    public VoidTearsHelmetModel(ModelPart root) {
        super(root);
        tearEyeCore = head.getChild("tear_eye_core");
        tearEyeLobe = head.getChild("tear_eye_lobe");
        tearCheekDrop = head.getChild("tear_cheek_drop");
        tearBrow = head.getChild("tear_brow");
        crownLeft = head.getChild("crown_spike_left");
        crownCenter = head.getChild("crown_spike_center");
        crownRight = head.getChild("crown_spike_right");
        tailFront = head.getChild("tail_front");
        tailMid = head.getChild("tail_mid");
        tailBack = head.getChild("tail_back");
        tailTip = head.getChild("tail_tip");
        dropOuter = head.getChild("drop_outer");
        dropLower = head.getChild("drop_lower");
        dropCrown = head.getChild("drop_crown");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition modelRoot = mesh.getRoot();
        // ArmorLayer expects the complete humanoid part tree. Keep every part empty
        // except head so the player's normal skin is never replaced by this texture.
        PartDefinition root = modelRoot.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        modelRoot.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // Vanilla mirrors the player model on X during rendering. The authored
        // negative-X anchor therefore lands on the player's right eye. All
        // pieces stay above the eye line so the plume never drops into the neck.
        add(root, "tear_eye_core", -3.8F, -4.8F, -5.2F, 3.6F, 3.2F, 2.1F, 0, 0, -8);
        add(root, "tear_eye_lobe", -4.9F, -4.0F, -4.8F, 2.6F, 3.2F, 2F, 0, 0, -22);
        add(root, "tear_cheek_drop", -3.8F, -2.8F, -4.7F, 2F, 2.8F, 1.7F, 0, 0, -12);
        add(root, "tear_brow", -4.0F, -5.0F, -3.8F, 3.5F, 1.8F, 2F, 0, 0, -18);
        // Crown tongues and the trailing plume are deliberately kept in front of
        // the head (negative Z).  They now sweep toward negative X/negative Y so
        // the silhouette reads as a single liquid flame flowing to the upper-left
        // instead of disappearing behind the player's head.
        add(root, "crown_spike_left", -6.0F, -9.5F, -4.15F, 2.0F, 4.5F, 1.8F, 0, -8, -42);
        add(root, "crown_spike_center", -4.4F, -10.0F, -4.0F, 1.9F, 5.0F, 1.8F, 0, -4, -28);
        add(root, "crown_spike_right", -2.8F, -8.5F, -3.85F, 1.8F, 3.8F, 1.7F, 0, 4, -16);
        add(root, "tail_front", -3.5F, -3.4F, -4.35F, 2.5F, 2.5F, 1.9F, -16, -10, -34);
        add(root, "tail_mid", -5.0F, -4.8F, -4.15F, 2.2F, 2.5F, 1.7F, -22, -14, -42);
        add(root, "tail_back", -6.5F, -6.1F, -3.95F, 1.9F, 2.3F, 1.5F, -28, -18, -50);
        add(root, "tail_tip", -8.0F, -7.4F, -3.75F, 1.5F, 2.0F, 1.3F, -34, -22, -58);
        add(root, "drop_outer", -7.0F, -5.3F, -4.25F, 1F, 1.4F, 1.1F, 0, 0, -38);
        add(root, "drop_lower", -8.4F, -6.8F, -4.05F, 1.2F, 1.4F, 1.1F, 0, 0, -50);
        add(root, "drop_crown", -9.6F, -8.4F, -3.85F, 1F, 1.3F, 1F, 0, 0, -60);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void add(PartDefinition parent, String name, float x, float y, float z,
                            float w, float h, float d, float rx, float ry, float rz) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(0, 0).addBox(-w / 2F, -h / 2F, -d / 2F, w, h, d),
                PartPose.offsetAndRotation(x + w / 2F, y + h / 2F, z + d / 2F,
                        rx * (float) Math.PI / 180F, ry * (float) Math.PI / 180F, rz * (float) Math.PI / 180F));
    }

    @Override public void setupAnim(Player entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * (float) Math.PI / 180F;
        head.xRot = headPitch * (float) Math.PI / 180F;

        // The liquid mass breathes slowly while each tongue follows a delayed wave.
        float liquid = Mth.sin(ageInTicks * 0.16F);
        float quick = Mth.sin(ageInTicks * 0.27F + 0.8F);
        tearEyeCore.zRot = rad(-8F) + liquid * 0.035F;
        tearEyeLobe.zRot = rad(-22F) - liquid * 0.055F;
        tearCheekDrop.zRot = rad(-12F) + quick * 0.05F;
        tearCheekDrop.y = -1.45F + Mth.sin(ageInTicks * 0.19F + 1.4F) * 0.18F;
        tearBrow.zRot = rad(-18F) + liquid * 0.045F;

        // The crown tongues all lean up/left.  Delayed phases make the crest
        // travel from the eye toward the outer tip instead of moving as one rigid
        // block.
        float crownWave = ageInTicks * 0.27F;
        crownLeft.xRot = Mth.sin(crownWave + 0.2F) * 0.07F;
        crownLeft.yRot = rad(-8F) + Mth.sin(crownWave + 0.2F) * 0.08F;
        crownLeft.zRot = rad(-42F) + Mth.sin(crownWave) * 0.16F;
        crownCenter.xRot = Mth.sin(crownWave + 1.0F) * 0.06F;
        crownCenter.yRot = rad(-4F) + Mth.sin(crownWave + 1.0F) * 0.07F;
        crownCenter.zRot = rad(-28F) + Mth.sin(crownWave + 1.0F) * 0.14F;
        crownRight.xRot = Mth.sin(crownWave + 2.0F) * 0.06F;
        crownRight.yRot = rad(4F) + Mth.sin(crownWave + 2.0F) * 0.07F;
        crownRight.zRot = rad(-16F) + Mth.sin(crownWave + 2.0F) * 0.13F;

        // A travelling wave runs along the four plume sections.  Negative base
        // yaw/pitch keeps every section in the visible upper-left quadrant;
        // translations and scale pulses sell a liquid, rather than mechanical,
        // motion.
        animateTail(tailFront, ageInTicks, 0.0F, -2.2F, -2.1F, -3.40F,
                -16F, -10F, -34F, 0.14F);
        animateTail(tailMid, ageInTicks, 0.62F, -3.65F, -3.45F, -3.30F,
                -22F, -14F, -42F, 0.18F);
        animateTail(tailBack, ageInTicks, 1.24F, -5.15F, -4.85F, -3.20F,
                -28F, -18F, -50F, 0.22F);
        animateTail(tailTip, ageInTicks, 1.86F, -6.65F, -6.25F, -3.05F,
                -34F, -22F, -58F, 0.26F);

        dropOuter.x = -6.5F + Mth.sin(ageInTicks * 0.24F) * 0.18F;
        dropOuter.y = -4.6F + Mth.cos(ageInTicks * 0.24F) * 0.26F;
        dropOuter.z = -3.7F + Mth.sin(ageInTicks * 0.20F + 0.8F) * 0.10F;
        dropLower.x = -7.8F + Mth.sin(ageInTicks * 0.20F + 2.1F) * 0.14F;
        dropLower.y = -6.0F + Mth.cos(ageInTicks * 0.20F + 2.1F) * 0.24F;
        dropLower.z = -3.50F + Mth.sin(ageInTicks * 0.18F + 1.1F) * 0.08F;
        dropCrown.x = -9.0F + Mth.sin(ageInTicks * 0.24F + 0.8F) * 0.18F;
        dropCrown.y = -7.8F + Mth.sin(ageInTicks * 0.26F + 4.0F) * 0.30F;
        dropCrown.z = -3.40F + Mth.cos(ageInTicks * 0.20F + 1.1F) * 0.12F;
    }

    private static void animateTail(ModelPart part, float time, float phase,
                                    float baseX, float baseY, float baseZ,
                                    float baseXRot, float baseYRot, float baseZRot,
                                    float drift) {
        float wave = Mth.sin(time * 0.30F - phase);
        float slow = Mth.sin(time * 0.17F - phase * 0.75F);
        part.xRot = rad(baseXRot) + wave * 0.11F;
        part.yRot = rad(baseYRot) + slow * 0.15F;
        part.zRot = rad(baseZRot) + Mth.sin(time * 0.24F - phase) * 0.14F;
        // Always derive the animated position from the authored position.  Using
        // += here would accumulate a sub-pixel offset every frame and make the
        // plume slowly drift away from the eye.
        part.x = baseX + Mth.sin(time * 0.22F - phase) * drift;
        part.y = baseY + Mth.cos(time * 0.26F - phase) * drift * 0.72F;
        part.z = baseZ + Mth.sin(time * 0.19F - phase + 0.7F) * drift * 0.35F;
        part.xScale = 1.0F + wave * drift * 0.10F;
        part.yScale = 1.0F + slow * drift * 0.16F;
        part.zScale = 1.0F + wave * drift * 0.08F;
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    @Override public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                         int packedOverlay, float r, float g, float b, float a) {
        head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
