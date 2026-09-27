package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.entity.PureFireBallEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 纯质阳炎·耀阳火球的球面渲染器。
 *
 * <p>这里直接绘制经纬度球面网格，不再使用互相穿插的公告板、方块碎片或原版火焰贴图。
 * 这样模型的轮廓始终是球形，贴图 UV 也只在 0~1 范围内走一遍。</p>
 */
public class PureFireBallRenderer extends EntityRenderer<PureFireBallEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            MJMod.MODID, "textures/entity/pure_fire_ball.png");
    private static final int LATITUDE_SEGMENTS = 16;
    private static final int LONGITUDE_SEGMENTS = 24;

    public PureFireBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(PureFireBallEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        LivingEntity owner = entity.getOwner();
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && owner == mc.player
                && entity.getState() == PureFireBallEntity.STATE_ORBIT) {
            Vec3 viewVec = mc.player.getViewVector(1.0F).normalize();
            Vec3 toBall = entity.position().subtract(mc.player.getEyePosition(1.0F)).normalize();
            if (viewVec.dot(toBall) > 0.72D) {
                return false;
            }
        }
        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    public void render(PureFireBallEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        boolean orbit = entity.getState() == PureFireBallEntity.STATE_ORBIT;
        float body = orbit ? 1.55F : 1.85F * entity.getChargeScale();
        float pulse = 1.0F + Mth.sin(age * 0.18F + entity.getOrbitIndex()) * 0.035F;
        float radius = body * pulse * 0.5F;
        int orbitIndex = entity.getOrbitIndex();
        float spinDirection = (orbitIndex & 1) == 0 ? 1.0F : -1.0F;

        poseStack.pushPose();
        // 整颗火球绕自己的轴独立自转；相邻火球反向并错开相位，避免八球同步转动。
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDirection * (age * 5.5F + orbitIndex * 31.0F)));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * (1.6F + orbitIndex * 0.12F) + orbitIndex * 17.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinDirection * (age * 1.1F + orbitIndex * 13.0F)));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        drawSphere(poseStack, consumer, radius);

        poseStack.popPose();
    }

    /** 绘制完整 UV 球：每个纬度带只使用一次 [0,1] UV，不会重复或滚出贴图边界。 */
    private static void drawSphere(PoseStack poseStack, VertexConsumer consumer, float radius) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (int lat = 0; lat < LATITUDE_SEGMENTS; lat++) {
            float v0 = lat / (float) LATITUDE_SEGMENTS;
            float v1 = (lat + 1) / (float) LATITUDE_SEGMENTS;
            float phi0 = (float) (Math.PI * lat / LATITUDE_SEGMENTS);
            float phi1 = (float) (Math.PI * (lat + 1) / LATITUDE_SEGMENTS);

            for (int lon = 0; lon < LONGITUDE_SEGMENTS; lon++) {
                float u0 = lon / (float) LONGITUDE_SEGMENTS;
                float u1 = (lon + 1) / (float) LONGITUDE_SEGMENTS;
                float theta0 = (float) (Math.PI * 2.0D * lon / LONGITUDE_SEGMENTS);
                float theta1 = (float) (Math.PI * 2.0D * (lon + 1) / LONGITUDE_SEGMENTS);

                // 顺序为左上、右上、右下、左下，保证球面正面朝外。
                sphereVertex(consumer, matrix, normalMatrix, radius, phi0, theta0, u0, v0, light, overlay);
                sphereVertex(consumer, matrix, normalMatrix, radius, phi0, theta1, u1, v0, light, overlay);
                sphereVertex(consumer, matrix, normalMatrix, radius, phi1, theta1, u1, v1, light, overlay);
                sphereVertex(consumer, matrix, normalMatrix, radius, phi1, theta0, u0, v1, light, overlay);
            }
        }
    }

    private static void sphereVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                     float radius, float phi, float theta,
                                     float u, float v, int light, int overlay) {
        float sinPhi = Mth.sin(phi);
        float nx = sinPhi * Mth.cos(theta);
        float ny = Mth.cos(phi);
        float nz = sinPhi * Mth.sin(theta);

        consumer.vertex(matrix, nx * radius, ny * radius, nz * radius)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, nx, ny, nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireBallEntity entity) {
        return TEXTURE;
    }
}
