package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mjmod.entity.ShenQingShuiZhiYaoEntity;
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
import net.minecraft.util.Mth;

/** Model converted from the supplied Blockbench geometry and 128x128 Box UV map. */
public class ShenQingShuiZhiYaoModel extends EntityModel<ShenQingShuiZhiYaoEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "shenqing_shuizhiyao"), "main");

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart hairRight;
    private final ModelPart hairCenter;
    private final ModelPart hairLeft;
    private final ModelPart frontCloth;
    private final ModelPart backCloth;

    public ShenQingShuiZhiYaoModel(ModelPart root) {
        this.root = root;
        this.waist = root.getChild("waist");
        this.head = waist.getChild("head");
        this.body = waist.getChild("body");
        this.rightArm = waist.getChild("right_arm");
        this.leftArm = waist.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.hairRight = head.getChild("hair_right");
        this.hairCenter = head.getChild("hair_center");
        this.hairLeft = head.getChild("hair_left");
        this.frontCloth = body.getChild("front_cloth");
        this.backCloth = body.getChild("back_cloth");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition head = waist.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 25).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        waist.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, -10.0F, 0.0F));

        waist.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 41).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, -10.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(16, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(32, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        head.addOrReplaceChild("hair_right", CubeListBuilder.create()
                        .texOffs(48, 0).addBox(-5.0F, -32.0F, -4.0F, 1.0F, 4.0F, 8.0F)
                        .texOffs(60, 25).addBox(-5.0F, -28.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(62, 30).addBox(-5.0F, -28.0F, -4.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(36, 59).addBox(-5.0F, -28.0F, -1.0F, 1.0F, 4.0F, 5.0F)
                        .texOffs(62, 35).addBox(-5.0F, -28.0F, -2.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        head.addOrReplaceChild("hair_center", CubeListBuilder.create()
                        .texOffs(24, 25).addBox(-5.0F, -33.0F, 4.0F, 10.0F, 18.0F, 1.0F)
                        .texOffs(48, 12).addBox(-4.0F, -33.0F, -5.0F, 8.0F, 3.0F, 1.0F)
                        .texOffs(52, 62).addBox(-2.0F, -30.0F, -5.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(0, 16).addBox(-5.0F, -33.0F, -4.0F, 10.0F, 1.0F, 8.0F)
                        .texOffs(36, 16).addBox(3.0F, -30.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(36, 21).addBox(2.0F, -30.0F, -5.0F, 1.0F, 3.0F, 1.0F)
                        .texOffs(60, 62).addBox(-4.0F, -26.0F, -5.0F, 1.0F, 5.0F, 1.0F)
                        .texOffs(40, 16).addBox(-4.0F, -30.0F, -5.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(56, 62).addBox(1.0F, -30.0F, -5.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(64, 25).addBox(3.0F, -26.0F, -5.0F, 1.0F, 5.0F, 1.0F)
                        .texOffs(40, 21).addBox(-3.0F, -30.0F, -5.0F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        head.addOrReplaceChild("hair_left", CubeListBuilder.create()
                        .texOffs(48, 50).addBox(-5.0F, -32.0F, -4.0F, 1.0F, 4.0F, 8.0F)
                        .texOffs(62, 40).addBox(-5.0F, -28.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(62, 45).addBox(-5.0F, -28.0F, -4.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(60, 16).addBox(-5.0F, -28.0F, -1.0F, 1.0F, 4.0F, 5.0F)
                        .texOffs(48, 62).addBox(-5.0F, -28.0F, -2.0F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(9.0F, 24.0F, 0.0F));

        waist.addOrReplaceChild("lower_garment", CubeListBuilder.create()
                        .texOffs(0, 59).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 12.0F, 1.0F)
                        .texOffs(48, 33).addBox(4.0F, -12.0F, -3.0F, 1.0F, 12.0F, 6.0F)
                        .texOffs(18, 59).addBox(-4.0F, -12.0F, 2.0F, 8.0F, 12.0F, 1.0F)
                        .texOffs(46, 16).addBox(-5.0F, -12.0F, -3.0F, 1.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        body.addOrReplaceChild("front_cloth", CubeListBuilder.create()
                        .texOffs(1, 1).addBox(0.0F, -21.0F, -3.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(-2, 1).addBox(-4.0F, -19.0F, -3.0F, 8.0F, 7.0F, 1.0F)
                        .texOffs(1, 1).addBox(-1.0F, -21.0F, -3.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(-1, 1).addBox(2.0F, -24.0F, -3.0F, 2.0F, 5.0F, 1.0F)
                        .texOffs(1, 1).addBox(-2.0F, -23.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(1, 1).addBox(1.0F, -23.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(-1, 1).addBox(-4.0F, -24.0F, -3.0F, 2.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        body.addOrReplaceChild("back_cloth", CubeListBuilder.create()
                        .texOffs(1, 1).addBox(0.0F, -21.0F, -3.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(-2, 1).addBox(-4.0F, -19.0F, -3.0F, 8.0F, 7.0F, 1.0F)
                        .texOffs(1, 1).addBox(-1.0F, -21.0F, -3.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(-1, 1).addBox(2.0F, -24.0F, -3.0F, 2.0F, 5.0F, 1.0F)
                        .texOffs(1, 1).addBox(-2.0F, -23.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(1, 1).addBox(1.0F, -23.0F, -3.0F, 1.0F, 4.0F, 1.0F)
                        .texOffs(-1, 1).addBox(-4.0F, -24.0F, -3.0F, 2.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 5.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ShenQingShuiZhiYaoEntity entity, float limbSwing,
                          float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);

        float walkAmount = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float walk = limbSwing * 0.6662F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        rightArm.xRot = Mth.cos(walk + Mth.PI) * 1.6F * walkAmount;
        leftArm.xRot = Mth.cos(walk) * 1.6F * walkAmount;
        rightLeg.xRot = Mth.cos(walk) * 1.4F * walkAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.4F * walkAmount;

        body.xRot = Mth.sin(ageInTicks * 0.08F) * 0.012F;
        float hairFlow = Mth.sin(ageInTicks * 0.075F) * 0.018F
                - Mth.cos(walk) * 0.025F * walkAmount;
        hairCenter.xRot = hairFlow;
        hairRight.xRot = hairFlow * 1.15F;
        hairLeft.xRot = hairFlow * 0.9F;
        hairRight.zRot = Mth.sin(ageInTicks * 0.055F) * 0.012F;
        hairLeft.zRot = -hairRight.zRot;

        float clothFlow = Mth.sin(ageInTicks * 0.09F) * 0.018F
                + Mth.cos(walk) * 0.055F * walkAmount;
        frontCloth.xRot = clothFlow;
        backCloth.xRot = -clothFlow * 0.75F;

        float strike = Mth.sin(Mth.sqrt(this.attackTime) * Mth.PI);
        if (strike > 0.0F) {
            waist.yRot = Mth.sin(this.attackTime * Mth.PI) * 0.18F;
            rightArm.xRot -= strike * 1.65F;
            rightArm.zRot = strike * 0.16F;
            leftArm.xRot += strike * 0.35F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light,
                               int overlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
    }
}
