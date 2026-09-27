package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mjmod.entity.EHuiShouEntity;
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

/** 由用户提供的 ehuishou.bbmodel 骨骼和 Box UV 数据转换。 */
public class EHuiShouModel extends EntityModel<EHuiShouEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "ehuishou"), "main");

    private final ModelPart creature;
    private final ModelPart head;
    private final ModelPart frontLegs;
    private final ModelPart rearLegs;

    public EHuiShouModel(ModelPart root) {
        this.creature = root.getChild("creature");
        this.head = creature.getChild("tou");
        this.frontLegs = creature.getChild("shou");
        this.rearLegs = creature.getChild("shou2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition creature = mesh.getRoot().addOrReplaceChild("creature",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(3.0F, -5.0F, -4.0F, 1.0F, 4.0F, 10.0F)
                        .texOffs(22, 15).addBox(-4.0F, -5.0F, -4.0F, 1.0F, 4.0F, 10.0F)
                        .texOffs(32, 5).addBox(-2.0F, -4.0F, 6.0F, 4.0F, 1.0F, 5.0F),
                // The source Blockbench model used the usual player-model
                // Y=24 pivot. At render scale 8 that would lift the whole
                // creature many blocks above its entity origin, so the pivot
                // is moved to the model's lower edge for proper alignment.
                PartPose.offset(0.0F, 8.0F, 0.0F));

        creature.addOrReplaceChild("tou", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -6.0F, -4.0F, 6.0F, 5.0F, 10.0F)
                        .texOffs(0, 29).addBox(-3.0F, -3.0F, -9.0F, 6.0F, 1.0F, 5.0F)
                        .texOffs(32, 0).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 2.0F, 3.0F)
                        .texOffs(32, 11).addBox(-3.0F, -6.0F, -6.0F, 6.0F, 1.0F, 2.0F)
                        .texOffs(44, 29).addBox(1.0F, -7.0F, -6.0F, 1.0F, 1.0F, 2.0F)
                        .texOffs(8, 41).addBox(3.0F, -5.0F, -7.0F, 1.0F, 2.0F, 3.0F)
                        .texOffs(16, 41).addBox(-4.0F, -5.0F, -7.0F, 1.0F, 2.0F, 3.0F)
                        .texOffs(44, 32).addBox(1.0F, -8.0F, -7.0F, 1.0F, 1.0F, 2.0F)
                        .texOffs(22, 29).addBox(-3.0F, -2.0F, -9.0F, 6.0F, 1.0F, 5.0F)
                        .texOffs(44, 35).addBox(-2.0F, -8.0F, -7.0F, 1.0F, 1.0F, 2.0F)
                        .texOffs(44, 38).addBox(-2.0F, -7.0F, -6.0F, 1.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        creature.addOrReplaceChild("shou", CubeListBuilder.create()
                        .texOffs(24, 41).addBox(4.0F, -5.0F, -4.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(32, 41).addBox(-6.0F, -5.0F, -4.0F, 2.0F, 3.0F, 2.0F)
                        .texOffs(40, 41).addBox(6.0F, -5.0F, -3.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(44, 14).addBox(-7.0F, -5.0F, -3.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(20, 35).addBox(7.0F, -5.0F, -3.0F, 1.0F, 3.0F, 3.0F)
                        .texOffs(28, 35).addBox(-8.0F, -5.0F, -3.0F, 1.0F, 3.0F, 3.0F),
                PartPose.ZERO);

        creature.addOrReplaceChild("shou2", CubeListBuilder.create()
                        .texOffs(36, 35).addBox(7.0F, -5.0F, -1.0F, 1.0F, 3.0F, 3.0F)
                        .texOffs(0, 35).addBox(4.0F, -5.0F, -3.0F, 2.0F, 3.0F, 3.0F)
                        .texOffs(44, 19).addBox(-7.0F, -5.0F, -1.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(10, 35).addBox(-6.0F, -5.0F, -3.0F, 2.0F, 3.0F, 3.0F)
                        .texOffs(44, 24).addBox(6.0F, -5.0F, -1.0F, 1.0F, 3.0F, 2.0F)
                        .texOffs(0, 41).addBox(-8.0F, -5.0F, -1.0F, 1.0F, 3.0F, 3.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EHuiShouEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        creature.getAllParts().forEach(ModelPart::resetPose);
        // Hovering flight cycle: the body gently bobs while the four side
        // appendages alternately sweep like fins. This replaces the old
        // ground-walking limb swing without changing the authored geometry.
        float hover = Mth.sin(ageInTicks * 0.085F) * 0.34F
                + Mth.sin(ageInTicks * 0.17F) * 0.10F;
        creature.y += hover;
        creature.xRot = Mth.sin(ageInTicks * 0.045F) * 0.035F;
        float flap = Mth.sin(ageInTicks * 0.24F) * 0.34F;
        frontLegs.xRot = flap;
        rearLegs.xRot = -flap * 0.82F;
        frontLegs.zRot = Mth.cos(ageInTicks * 0.24F) * 0.10F;
        rearLegs.zRot = -frontLegs.zRot * 0.8F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.18F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.12F;

        if (entity.isCharging()) {
            // A short, readable dive posture while the server moves the mob.
            creature.xRot = -0.34F;
            creature.z -= 0.35F;
            head.xRot -= 0.20F;
            frontLegs.xRot = -0.58F;
            rearLegs.xRot = -0.34F;
            frontLegs.zRot = 0.04F;
            rearLegs.zRot = -0.04F;
        } else {
            float strike = Mth.sin(Mth.sqrt(this.attackTime) * Mth.PI);
            head.xRot -= strike * 0.38F;
            frontLegs.xRot -= strike * 0.45F;
            creature.z -= strike * 0.55F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int light,
                               int overlay, float red, float green, float blue, float alpha) {
        creature.render(poseStack, consumer, light, overlay, red, green, blue, alpha);
    }
}
