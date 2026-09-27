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
 * 虚空之泪专用施法动画模型，只绘制淡蓝色水波片与泪滴。
 */
public class VoidTearCastAnimationModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "void_tear_cast_animation"), "main");

    private final ModelPart waveLeft;
    private final ModelPart waveCenter;
    private final ModelPart waveRight;
    private final ModelPart dropTop;
    private final ModelPart dropBottom;

    public VoidTearCastAnimationModel(ModelPart root) {
        waveLeft = root.getChild("wave_left");
        waveCenter = root.getChild("wave_center");
        waveRight = root.getChild("wave_right");
        dropTop = root.getChild("drop_top");
        dropBottom = root.getChild("drop_bottom");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("wave_left", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-13.0F, -15.0F, -1.0F, 3.0F, 30.0F, 2.0F), PartPose.offset(-0.5F, 0.0F, 0.0F));
        root.addOrReplaceChild("wave_center", CubeListBuilder.create().texOffs(8, 0)
                .addBox(-7.0F, -18.0F, -1.0F, 3.0F, 36.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("wave_right", CubeListBuilder.create().texOffs(16, 0)
                .addBox(10.0F, -15.0F, -1.0F, 3.0F, 30.0F, 2.0F), PartPose.offset(0.5F, 0.0F, 0.0F));
        root.addOrReplaceChild("drop_top", CubeListBuilder.create().texOffs(24, 0)
                .addBox(-2.0F, -5.0F, -1.5F, 4.0F, 7.0F, 3.0F), PartPose.offset(4.0F, -18.0F, 0.0F));
        root.addOrReplaceChild("drop_bottom", CubeListBuilder.create().texOffs(24, 10)
                .addBox(-1.5F, -4.0F, -1.25F, 3.0F, 6.0F, 2.5F), PartPose.offset(-5.0F, 17.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public void animate(float time, float progress) {
        float wave = (float) Math.sin(time * 0.32F);
        waveLeft.yRot = wave * 0.16F;
        waveCenter.yRot = (float) Math.sin(time * 0.32F + 1.1F) * 0.18F;
        waveRight.yRot = (float) Math.sin(time * 0.32F + 2.2F) * 0.16F;
        waveLeft.z = -1.0F + (float) Math.sin(time * 0.25F) * 1.3F;
        waveCenter.z = (float) Math.sin(time * 0.25F + 1.1F) * 1.3F;
        waveRight.z = 1.0F + (float) Math.sin(time * 0.25F + 2.2F) * 1.3F;
        float pulse = 0.70F + progress * 0.30F;
        waveLeft.yScale = pulse;
        waveCenter.yScale = pulse;
        waveRight.yScale = pulse;
        dropTop.y = -18.0F + (float) Math.sin(time * 0.40F) * 2.2F;
        dropBottom.y = 17.0F + (float) Math.cos(time * 0.36F + 0.8F) * 2.0F;
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        waveLeft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        waveCenter.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        waveRight.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        dropTop.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        dropBottom.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
