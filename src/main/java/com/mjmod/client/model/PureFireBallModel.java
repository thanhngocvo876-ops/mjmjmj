package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

/**
 * 纯质阳炎·金面烈阳火球 3D Blockbench 模型：
 * 内核 + 翻滚火幔 + 十字与对角微缩日冕耀斑薄片
 */
public class PureFireBallModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_ball"), "main");
    public static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/pure_fire_ball.png");

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart mantle;
    private final ModelPart corona1;
    private final ModelPart corona2;
    private final ModelPart coronaDiag1;
    private final ModelPart coronaDiag2;

    public PureFireBallModel(ModelPart root) {
        this.root = root.getChild("fire_ball_root");
        this.core = this.root.getChild("core");
        this.mantle = this.root.getChild("mantle");
        this.corona1 = this.root.getChild("corona_cross_1");
        this.corona2 = this.root.getChild("corona_cross_2");
        this.coronaDiag1 = this.root.getChild("corona_diag_1");
        this.coronaDiag2 = this.root.getChild("corona_diag_2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition fireBallRoot = root.addOrReplaceChild("fire_ball_root",
                CubeListBuilder.create(), PartPose.ZERO);

        // 中心高亮火核 (6x6x6)
        fireBallRoot.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        // 翻滚熔岩幔 (8x8x8)
        fireBallRoot.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        // 十字耀斑面片 1 (14x14)
        fireBallRoot.addOrReplaceChild("corona_cross_1",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-7.0F, -7.0F, 0.0F, 14.0F, 14.0F, 0.0F),
                PartPose.ZERO);

        // 十字耀斑面片 2 (14x14)
        fireBallRoot.addOrReplaceChild("corona_cross_2",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(0.0F, -7.0F, -7.0F, 0.0F, 14.0F, 14.0F),
                PartPose.ZERO);

        // 对角耀斑面片 1 (45度倾斜)
        fireBallRoot.addOrReplaceChild("corona_diag_1",
                CubeListBuilder.create().texOffs(32, 32)
                        .addBox(-7.0F, -7.0F, 0.0F, 14.0F, 14.0F, 0.0F),
                PartPose.rotation(0.0F, (float) Math.toRadians(45.0D), 0.0F));

        // 对角耀斑面片 2
        fireBallRoot.addOrReplaceChild("corona_diag_2",
                CubeListBuilder.create().texOffs(32, 32)
                        .addBox(0.0F, -7.0F, -7.0F, 0.0F, 14.0F, 14.0F),
                PartPose.rotation(0.0F, (float) Math.toRadians(45.0D), 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float pulse = Mth.sin(ageInTicks * 0.2F);
        core.xScale = 1.0F + pulse * 0.06F;
        core.yScale = 1.0F + pulse * 0.06F;
        core.zScale = 1.0F + pulse * 0.06F;

        mantle.yRot = ageInTicks * 0.08F;
        mantle.xRot = ageInTicks * 0.05F;

        corona1.zRot = ageInTicks * 0.12F;
        corona2.xRot = -ageInTicks * 0.12F;
        coronaDiag1.zRot = -ageInTicks * 0.10F;
        coronaDiag2.xRot = ageInTicks * 0.10F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float r, float g, float b, float a) {
        root.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
