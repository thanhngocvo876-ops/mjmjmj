package com.mjmod.client.model;

import com.mjmod.MJMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 纯质阳炎·火焰拳套 —— 由 blockbench_models/generate_pure_fire_gauntlet.ps1 生成的
 * Blockbench 工程（pure_fire_gauntlet.bbmodel）镜像而来。金甲拳套包裹拳部，
 * 三层火焰沿小臂向上升腾：白炽焰芯（贴近拳面）、炽橙中焰、赤红半透明外焰，
 * 顶端另有脱离拳体的盘旋火星，对应参考图里"双手缠焰"的造型。
 *
 * <p>火焰流动感的关键在 {@link #animateSide}：每根火舌的波动相位随其高度
 * 递减，形成一道自下而上掠过整簇火焰的行波；外层摆幅大于内层、火尖大于
 * 焰根，并叠加火舌上蹿、明暗呼吸与缓慢盘旋的火星。
 */
public final class PureFireGauntletModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_gauntlet"), "main");

    private final ModelPart leftGauntlet;
    private final ModelPart rightGauntlet;
    private final ModelPart[] leftParts;
    private final ModelPart[] rightParts;

    public PureFireGauntletModel(ModelPart root) {
        leftGauntlet = root.getChild("left_gauntlet");
        rightGauntlet = root.getChild("right_gauntlet");
        leftParts = parts(leftGauntlet, LEFT_BOXES.length);
        rightParts = parts(rightGauntlet, RIGHT_BOXES.length);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSide(root, "left_gauntlet", LEFT_BOXES);
        addSide(root, "right_gauntlet", RIGHT_BOXES);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addSide(PartDefinition root, String name, Box[] boxes) {
        PartDefinition side = root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < boxes.length; i++) {
            Box box = boxes[i];
            side.addOrReplaceChild("cube_" + i,
                    CubeListBuilder.create().texOffs(box.u(), box.v())
                            .addBox(box.x(), box.y(), box.z(), box.dx(), box.dy(), box.dz()),
                    // bbmodel 里中焰/外焰的静态外翻卷曲在这里以初始姿态还原
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, box.curl()));
        }
    }

    private static ModelPart[] parts(ModelPart side, int count) {
        ModelPart[] result = new ModelPart[count];
        for (int i = 0; i < result.length; i++) {
            result[i] = side.getChild("cube_" + i);
        }
        return result;
    }

    public void animate(float ageInTicks) {
        animateSide(leftGauntlet, leftParts, ageInTicks * 0.16F, true);
        animateSide(rightGauntlet, rightParts, ageInTicks * 0.16F, false);
    }

    /** Plays the 直冲拳 clip: restrained flame ripple during charge, frozen on hold. */
    public void animatePunch(float clip, int phase, float ageInTicks) {
        animatePunchSide(leftGauntlet, leftParts, LEFT_BOXES, clip, phase, true);
        animatePunchSide(rightGauntlet, rightParts, RIGHT_BOXES, clip, phase, false);
    }

    private static void animatePunchSide(ModelPart side, ModelPart[] parts, Box[] boxes,
                                         float clip, int phase, boolean left) {
        side.resetPose();
        for (ModelPart part : parts) part.resetPose();
        // The arm rotations come from the Player Animator keyframes; only the
        // authored flame pieces ripple. Phase 1 (hold) freezes the ripple.
        float time = phase == 1 ? 1.0F : clip;
        float sideSign = left ? -1.0F : 1.0F;
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            if (boxes[i].layer() == 0) continue;
            float phaseOffset = time * 2.0F + i * 0.24F + (left ? 0.0F : 0.15F);
            float wave = Mth.sin(phaseOffset);
            float edge = 0.35F + 0.65F * boxes[i].rise();
            part.x += sideSign * wave * (0.018F + edge * 0.025F);
            part.y += Mth.cos(phaseOffset * 0.80F) * (0.022F + edge * 0.028F);
            part.z += Mth.sin(phaseOffset * 0.65F) * 0.018F;
            part.zRot += wave * 0.018F;
            float flick = 1.0F + Mth.sin(phaseOffset * 1.12F) * 0.014F;
            part.xScale = flick;
            part.yScale = 1.0F + wave * 0.018F;
            part.zScale = flick;
        }
    }

    private static void animateSide(ModelPart side, ModelPart[] parts, float time, boolean left) {
        side.resetPose();
        float pulse = 1.0F + Mth.sin(time * 1.35F) * 0.022F;
        side.xScale = pulse;
        side.yScale = pulse;
        side.zScale = pulse;
        for (int i = 0; i < parts.length; i++) {
            ModelPart part = parts[i];
            part.resetPose();
            Box box = (left ? LEFT_BOXES : RIGHT_BOXES)[i];
            int layer = box.layer();
            if (layer == 0) continue; // 金甲只随整体呼吸
            float rise = box.rise();
            // 行波：相位随高度递减，波峰自下而上扫过整簇火焰
            float phase = time * 2.6F - rise * 3.0F + (left ? 0.0F : 1.6F) + i * 0.17F;
            float wave = Mth.sin(phase);
            float reach = 0.35F + 0.65F * rise;
            float sway = layer == 1 ? 0.028F : layer == 2 ? 0.050F : 0.070F;
            part.x += wave * sway * reach + Mth.cos(phase * 0.71F) * sway * 0.5F;
            part.z += Mth.sin(phase * 0.83F + i * 0.9F) * sway * reach;
            // 火舌上蹿：波峰经过时向上升起一段
            part.y -= Math.max(0.0F, Mth.sin(phase * 0.92F)) * 0.055F * reach;
            part.zRot += wave * 0.055F * reach;
            part.yRot += Mth.cos(phase * 0.64F) * 0.05F * reach;
            float flick = 1.0F + Mth.sin(phase * 1.33F) * (0.028F + 0.034F * rise);
            part.xScale = flick;
            part.yScale = 1.0F + wave * (0.04F + 0.055F * rise);
            part.zScale = flick;
            if (layer == 4) {
                // 脱离的火星绕拳缓慢盘旋、上下浮沉
                part.yRot += time * 0.85F + i * 2.6F;
                part.y += Mth.sin(time * 1.8F + i * 1.3F) * 0.14F;
            }
        }
    }

    public void renderLeft(PoseStack poseStack, VertexConsumer consumer, int light,
                           int overlay, float r, float g, float b, float alpha) {
        leftGauntlet.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public void renderRight(PoseStack poseStack, VertexConsumer consumer, int light,
                            int overlay, float r, float g, float b, float alpha) {
        rightGauntlet.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    private record Box(float x, float y, float z, float dx, float dy, float dz,
                       int u, int v, int layer, float rise, float curl) {}

    // 以下数组由 generate_pure_fire_gauntlet.ps1 生成，勿手改；调整造型请改脚本后重新生成。
    private static final Box[] LEFT_BOXES = {
        new Box(-3F, 0F, -2F, 6F, 2F, 4F, 0, 0, 0, 0F, 0F),
        new Box(-3F, -1F, -3F, 6F, 1F, 2F, 0, 8, 0, 0F, 0F),
        new Box(-3F, -2F, -2F, 6F, 1F, 4F, 0, 12, 0, 0F, 0F),
        new Box(-3F, 2F, -2F, 6F, 1F, 4F, 0, 18, 0, 0F, 0F),
        new Box(-4F, 0F, -1F, 1F, 2F, 2F, 24, 0, 0, 0F, 0F),
        new Box(3F, 0F, -1F, 1F, 2F, 2F, 24, 4, 0, 0F, 0F),
        new Box(-2F, -7F, 0F, 2F, 5F, 2F, 24, 8, 1, 0.5833F, 0F),
        new Box(0F, -6F, -1F, 2F, 4F, 2F, 24, 8, 1, 0.5F, 0F),
        new Box(-1F, -5F, -3F, 2F, 3F, 2F, 24, 8, 1, 0.4167F, 0F),
        new Box(-1F, -5F, 1F, 2F, 3F, 2F, 24, 8, 1, 0.4167F, 0F),
        new Box(-1.5F, -8.5F, 0.5F, 1F, 2F, 1F, 24, 24, 1, 0.7083F, 0F),
        new Box(0.5F, -7.5F, -0.5F, 1F, 1F, 1F, 24, 24, 1, 0.625F, 0F),
        new Box(-0.5F, -7.5F, -2.5F, 1F, 1F, 1F, 24, 26, 1, 0.625F, 0F),
        new Box(-4F, -9F, -2F, 2F, 7F, 2F, 40, 0, 2, 0.75F, -0.0873F),
        new Box(2F, -8F, -2F, 2F, 6F, 2F, 40, 0, 2, 0.6667F, 0.0873F),
        new Box(-4F, -7F, 1F, 2F, 5F, 2F, 40, 0, 2, 0.5833F, 0F),
        new Box(2F, -6F, 1F, 2F, 4F, 2F, 40, 0, 2, 0.5F, 0F),
        new Box(-1F, -8F, -4F, 2F, 6F, 1F, 40, 0, 2, 0.6667F, 0F),
        new Box(-1F, -7F, 3F, 2F, 5F, 1F, 40, 0, 2, 0.5833F, 0F),
        new Box(-3.5F, -10.5F, -1.5F, 1F, 2F, 1F, 40, 12, 2, 0.875F, -0.1571F),
        new Box(2.5F, -9.5F, -1.5F, 1F, 1F, 1F, 40, 12, 2, 0.7917F, 0.1396F),
        new Box(-0.5F, -9.5F, -4.5F, 1F, 1F, 1F, 40, 14, 2, 0.7917F, 0F),
        new Box(-6F, -11F, -2F, 2F, 9F, 2F, 0, 32, 3, 0.9167F, -0.1222F),
        new Box(4F, -10F, -1F, 2F, 8F, 2F, 0, 32, 3, 0.8333F, 0.1222F),
        new Box(-5F, -8F, 2F, 2F, 6F, 2F, 0, 32, 3, 0.6667F, 0F),
        new Box(3F, -7F, 2F, 2F, 5F, 2F, 0, 32, 3, 0.5833F, 0F),
        new Box(-2F, -10F, -5F, 2F, 8F, 2F, 0, 32, 3, 0.8333F, 0F),
        new Box(-2F, -8F, 4F, 2F, 6F, 2F, 0, 32, 3, 0.6667F, 0F),
        new Box(-6.5F, -12.5F, -1.5F, 1F, 2F, 1F, 16, 32, 3, 1F, -0.192F),
        new Box(4.5F, -11.5F, -0.5F, 1F, 1F, 1F, 16, 32, 3, 0.9583F, 0.1745F),
        new Box(-7F, -14F, -1F, 1F, 2F, 1F, 16, 36, 4, 1F, 0F),
        new Box(5F, -13F, 2F, 1F, 1F, 1F, 16, 36, 4, 1F, 0F),
    };

    private static final Box[] RIGHT_BOXES = {
        new Box(-3F, 0F, -2F, 6F, 2F, 4F, 0, 0, 0, 0F, 0F),
        new Box(-3F, -1F, -3F, 6F, 1F, 2F, 0, 8, 0, 0F, 0F),
        new Box(-3F, -2F, -2F, 6F, 1F, 4F, 0, 12, 0, 0F, 0F),
        new Box(-3F, 2F, -2F, 6F, 1F, 4F, 0, 18, 0, 0F, 0F),
        new Box(3F, 0F, -1F, 1F, 2F, 2F, 24, 0, 0, 0F, 0F),
        new Box(-4F, 0F, -1F, 1F, 2F, 2F, 24, 4, 0, 0F, 0F),
        new Box(0F, -7F, 0F, 2F, 5F, 2F, 24, 8, 1, 0.5833F, 0F),
        new Box(-2F, -6F, -1F, 2F, 4F, 2F, 24, 8, 1, 0.5F, 0F),
        new Box(-1F, -5F, -3F, 2F, 3F, 2F, 24, 8, 1, 0.4167F, 0F),
        new Box(-1F, -5F, 1F, 2F, 3F, 2F, 24, 8, 1, 0.4167F, 0F),
        new Box(0.5F, -8.5F, 0.5F, 1F, 2F, 1F, 24, 24, 1, 0.7083F, 0F),
        new Box(-1.5F, -7.5F, -0.5F, 1F, 1F, 1F, 24, 24, 1, 0.625F, 0F),
        new Box(-0.5F, -7.5F, -2.5F, 1F, 1F, 1F, 24, 26, 1, 0.625F, 0F),
        new Box(2F, -9F, -2F, 2F, 7F, 2F, 40, 0, 2, 0.75F, 0.0873F),
        new Box(-4F, -8F, -2F, 2F, 6F, 2F, 40, 0, 2, 0.6667F, -0.0873F),
        new Box(2F, -7F, 1F, 2F, 5F, 2F, 40, 0, 2, 0.5833F, 0F),
        new Box(-4F, -6F, 1F, 2F, 4F, 2F, 40, 0, 2, 0.5F, 0F),
        new Box(-1F, -8F, -4F, 2F, 6F, 1F, 40, 0, 2, 0.6667F, 0F),
        new Box(-1F, -7F, 3F, 2F, 5F, 1F, 40, 0, 2, 0.5833F, 0F),
        new Box(2.5F, -10.5F, -1.5F, 1F, 2F, 1F, 40, 12, 2, 0.875F, 0.1571F),
        new Box(-3.5F, -9.5F, -1.5F, 1F, 1F, 1F, 40, 12, 2, 0.7917F, -0.1396F),
        new Box(-0.5F, -9.5F, -4.5F, 1F, 1F, 1F, 40, 14, 2, 0.7917F, 0F),
        new Box(4F, -11F, -2F, 2F, 9F, 2F, 0, 32, 3, 0.9167F, 0.1222F),
        new Box(-6F, -10F, -1F, 2F, 8F, 2F, 0, 32, 3, 0.8333F, -0.1222F),
        new Box(3F, -8F, 2F, 2F, 6F, 2F, 0, 32, 3, 0.6667F, 0F),
        new Box(-5F, -7F, 2F, 2F, 5F, 2F, 0, 32, 3, 0.5833F, 0F),
        new Box(0F, -10F, -5F, 2F, 8F, 2F, 0, 32, 3, 0.8333F, 0F),
        new Box(0F, -8F, 4F, 2F, 6F, 2F, 0, 32, 3, 0.6667F, 0F),
        new Box(5.5F, -12.5F, -1.5F, 1F, 2F, 1F, 16, 32, 3, 1F, 0.192F),
        new Box(-5.5F, -11.5F, -0.5F, 1F, 1F, 1F, 16, 32, 3, 0.9583F, -0.1745F),
        new Box(6F, -14F, -1F, 1F, 2F, 1F, 16, 36, 4, 1F, 0F),
        new Box(-6F, -13F, 2F, 1F, 1F, 1F, 16, 36, 4, 1F, 0F),
    };
}
