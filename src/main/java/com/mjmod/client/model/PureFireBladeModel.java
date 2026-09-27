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
 * Pure Fire Blade model extracted from 火刃.bbmodel.
 * The model consists of Right Arm attachment:
 * - hr: base blade bone (10 cubes)
 * - hr2: angled forward blade wing (10 cubes, Y rot: +30 deg)
 * - hr3: angled backward blade wing (10 cubes, Y rot: -30 deg)
 * Root is anchored directly to the player's right hand/arm tip.
 * Coordinates are normalized around right arm hand pivot.
 */
public final class PureFireBladeModel {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(MJMod.MODID, "pure_fire_blade"), "main");

    public static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/effect/pure_fire_blade.png");

    private final ModelPart root;
    private final ModelPart hr;
    private final ModelPart hr2;
    private final ModelPart hr3;

    public PureFireBladeModel(ModelPart root) {
        this.root = root.getChild("blade_root");
        this.hr = this.root.getChild("hr");
        this.hr2 = this.root.getChild("hr2");
        this.hr3 = this.root.getChild("hr3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootPart = mesh.getRoot();

        // Right Arm in Blockbench: from=[4, 12, -2] to=[8, 24, 2].
        // Arm center in X is 6, Z is 0. Hand bottom is Y=12 in Blockbench (or arm tip).
        // hr cubes are around X=3~10, Y=7~17, Z=-3~4.
        // Hand center offset: x0 = 6, y0 = 12, z0 = 0.
        // In Minecraft ModelPart space:
        // When attached to rightArm after arm.translateAndRotate:
        // vanilla arm pivot is (-5, 2, 0), local coordinates: x: -2..2, y: 0..12, z: -2..2.
        // Hand tip is at y = 10..12.
        // Here we create blade_root with offset to align with right arm hand.
        PartDefinition bladeRoot = rootPart.addOrReplaceChild("blade_root",
                CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));

        // Group hr: origin [0,0,0], rot [0,0,0]
        PartDefinition hrPart = bladeRoot.addOrReplaceChild("hr",
                CubeListBuilder.create(), PartPose.ZERO);
        addBoxes(hrPart, HR_BOXES);

        // Group hr2: origin [0,0,3], rot [0, 30 deg, 0]
        // Relative to hand center: origin z is offset
        PartDefinition hr2Part = bladeRoot.addOrReplaceChild("hr2",
                CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F, 0.0F));
        addBoxes(hr2Part, HR2_BOXES);

        // Group hr3: origin [1,0,-3], rot [0, -30 deg, 0]
        PartDefinition hr3Part = bladeRoot.addOrReplaceChild("hr3",
                CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F, 0.0F));
        addBoxes(hr3Part, HR3_BOXES);

        return LayerDefinition.create(mesh, 32, 32);
    }

    private static void addBoxes(PartDefinition part, BoxData[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            BoxData b = boxes[i];
            part.addOrReplaceChild("cube_" + i,
                    CubeListBuilder.create().texOffs(b.u, b.v)
                            .addBox(b.x, b.y, b.z, b.dx, b.dy, b.dz, new CubeDeformation(0.0F)),
                    PartPose.ZERO);
        }
    }

    public void animate(float ageInTicks, float intensity) {
        hr.resetPose();
        hr2.resetPose();
        hr3.resetPose();

        float wave = Mth.sin(ageInTicks * 0.25F) * 0.04F * intensity;
        float wave2 = Mth.cos(ageInTicks * 0.3F) * 0.04F * intensity;

        hr.yRot = wave;
        hr2.yRot = 0.5235988F + wave2;
        hr3.yRot = -0.5235988F - wave;

        float pulse = 1.0F + Mth.sin(ageInTicks * 0.4F) * 0.02F * intensity;
        root.xScale = pulse;
        root.yScale = pulse;
        root.zScale = pulse;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light,
                       int overlay, float r, float g, float b, float alpha) {
        root.render(poseStack, consumer, light, overlay, r, g, b, alpha);
    }

    public ModelPart getRoot() {
        return root;
    }

    private record BoxData(float x, float y, float z, float dx, float dy, float dz, int u, int v) {}

    // Normalized relative to right hand center (x0 = 6, y0 = 12, z0 = 0)
    // Blockbench cube coords: from [x1, y1, z1] to [x2, y2, z2]
    // Hand local coords: x = x1 - 6, y = y1 - 12 (note BB Y is upward, MC ModelPart Y is downward)
    // In MC ModelPart: +Y is downward, so y_mc = -(y2 - 12), dy = y2 - y1
    // Let's accurately map:
    // BB box: from [x1, y1, z1] to [x2, y2, z2].
    // Dimensions: dx = x2 - x1, dy = y2 - y1, dz = z2 - z1.
    // Origin in BB: hand is at (6, 12, 0).
    // Let's compute (x1 - 6, -(y2 - 12), z1 - 0).
    // For HR:
    private static final BoxData[] HR_BOXES = {
        new BoxData(5F - 6F, -(9F - 12F), 0F, 1F, 2F, 1F, 0, 12), // ee7b3413 from=[5,7,0] to=[6,9,1]
        new BoxData(7F - 6F, -(11F - 12F), 0F, 1F, 2F, 1F, 12, 0), // 7ee4e5ec from=[7,9,0] to=[8,11,1]
        new BoxData(8F - 6F, -(17F - 12F), 0F, 1F, 5F, 1F, 0, 0),  // 776348f7 from=[8,12,0] to=[9,17,1] (blade edge)
        new BoxData(6F - 6F, -(9F - 12F), 0F, 1F, 2F, 1F, 12, 3), // 0e4a935a from=[6,7,0] to=[7,9,1]
        new BoxData(5F - 6F, -(10F - 12F), 0F, 1F, 1F, 1F, 16, 0), // 312e5b2e from=[5,9,0] to=[6,10,1]
        new BoxData(3F - 6F, -(17F - 12F), 0F, 1F, 5F, 1F, 4, 0),  // 3b162457 from=[3,12,0] to=[4,17,1] (blade back)
        new BoxData(4F - 6F, -(12F - 12F), 0F, 1F, 1F, 1F, 16, 2), // c1cfc1b0 from=[4,11,0] to=[5,12,1]
        new BoxData(4F - 6F, -(11F - 12F), 0F, 1F, 2F, 1F, 4, 12), // 5db38102 from=[4,9,0] to=[5,11,1]
        new BoxData(7F - 6F, -(12F - 12F), 0F, 1F, 1F, 1F, 16, 4), // 787d2ed2 from=[7,11,0] to=[8,12,1]
        new BoxData(6F - 6F, -(10F - 12F), 0F, 1F, 1F, 1F, 16, 6)  // e3d868c8 from=[6,9,0] to=[7,10,1]
    };

    private static final BoxData[] HR2_BOXES = {
        new BoxData(5F - 6F, -(9F - 12F), 3F - 3F, 1F, 2F, 1F, 12, 6),  // 084832ce from=[5,7,3] to=[6,9,4]
        new BoxData(7F - 6F, -(11F - 12F), 3F - 3F, 1F, 2F, 1F, 8, 12), // e48e3889 from=[7,9,3] to=[8,11,4]
        new BoxData(8F - 6F, -(17F - 12F), 3F - 3F, 1F, 5F, 1F, 0, 6),  // 5f764dcc from=[8,12,3] to=[9,17,4]
        new BoxData(6F - 6F, -(9F - 12F), 3F - 3F, 1F, 2F, 1F, 12, 9),  // 68b9f200 from=[6,7,3] to=[7,9,4]
        new BoxData(5F - 6F, -(10F - 12F), 3F - 3F, 1F, 1F, 1F, 16, 8),  // 25fa0d79 from=[5,9,3] to=[6,10,4]
        new BoxData(3F - 6F, -(17F - 12F), 3F - 3F, 1F, 5F, 1F, 4, 6),  // 3f51c213 from=[3,12,3] to=[4,17,4]
        new BoxData(4F - 6F, -(12F - 12F), 3F - 3F, 1F, 1F, 1F, 16, 10), // a2dbfe62 from=[4,11,3] to=[5,12,4]
        new BoxData(4F - 6F, -(11F - 12F), 3F - 3F, 1F, 2F, 1F, 12, 12), // 69855a95 from=[4,9,3] to=[5,11,4]
        new BoxData(7F - 6F, -(12F - 12F), 3F - 3F, 1F, 1F, 1F, 16, 12), // efedd5ca from=[7,11,3] to=[8,12,4]
        new BoxData(6F - 6F, -(10F - 12F), 3F - 3F, 1F, 1F, 1F, 16, 14)  // 6365cdf3 from=[6,9,3] to=[7,10,4]
    };

    private static final BoxData[] HR3_BOXES = {
        new BoxData(6F - 7F, -(9F - 12F), -3F - (-3F), 1F, 2F, 1F, 0, 15),  // cb9a23d4 from=[6,7,-3] to=[7,9,-2]
        new BoxData(8F - 7F, -(11F - 12F), -3F - (-3F), 1F, 2F, 1F, 4, 15), // 3d7caa1f from=[8,9,-3] to=[9,11,-2]
        new BoxData(9F - 7F, -(17F - 12F), -3F - (-3F), 1F, 5F, 1F, 8, 0),  // 69a84bc8 from=[9,12,-3] to=[10,17,-2]
        new BoxData(7F - 7F, -(9F - 12F), -3F - (-3F), 1F, 2F, 1F, 8, 15),  // 6ad7fe47 from=[7,7,-3] to=[8,9,-2]
        new BoxData(6F - 7F, -(10F - 12F), -3F - (-3F), 1F, 1F, 1F, 16, 16), // 2979debe from=[6,9,-3] to=[7,10,-2]
        new BoxData(4F - 7F, -(17F - 12F), -3F - (-3F), 1F, 5F, 1F, 8, 6),  // d93a948e from=[4,12,-3] to=[5,17,-2]
        new BoxData(5F - 7F, -(12F - 12F), -3F - (-3F), 1F, 1F, 1F, 0, 18),  // 4fb2b789 from=[5,11,-3] to=[6,12,-2]
        new BoxData(5F - 7F, -(11F - 12F), -3F - (-3F), 1F, 2F, 1F, 12, 15), // 8972e0a6 from=[5,9,-3] to=[6,11,-2]
        new BoxData(8F - 7F, -(12F - 12F), -3F - (-3F), 1F, 1F, 1F, 4, 18),  // 10c1ae0a from=[8,11,-3] to=[9,12,-2]
        new BoxData(7F - 7F, -(10F - 12F), -3F - (-3F), 1F, 1F, 1F, 8, 18)   // 39c463de from=[7,9,-3] to=[8,10,-2]
    };
}
