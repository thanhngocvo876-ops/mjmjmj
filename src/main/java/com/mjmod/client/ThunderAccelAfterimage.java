package com.mjmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 闪电侠源码的残影是客户端假实体。这里不生成实体：走路时把位置存进队列，
 * 约 6 tick 后再画。取消玩家渲染之后 Post 不会跑，所以虚形画在世界阶段。
 * 残影碰不到、不挡路。自己第一人称不画。
 */
public final class ThunderAccelAfterimage {
    public static final int DELAY_TICKS = 6;
    private static final int MAX_SAMPLES = 24;
    private static final Map<UUID, Deque<Sample>> TRAILS = new ConcurrentHashMap<>();
    private static PlayerModel<AbstractClientPlayer> wideModel;
    private static PlayerModel<AbstractClientPlayer> slimModel;

    private ThunderAccelAfterimage() {}

    public static void tick(Player player, boolean active) {
        UUID id = player.getUUID();
        if (!active || !player.isAlive()) {
            TRAILS.remove(id);
            return;
        }
        Deque<Sample> trail = TRAILS.computeIfAbsent(id, ignored -> new ArrayDeque<>());
        Sample last = trail.peekLast();
        if (last == null || player.distanceToSqr(last.x, last.y, last.z) > 0.04D) {
            trail.addLast(Sample.capture(player));
            while (trail.size() > MAX_SAMPLES) trail.removeFirst();
        }
        int now = player.tickCount;
        Iterator<Sample> it = trail.iterator();
        while (it.hasNext()) {
            if (now - it.next().tick > DELAY_TICKS + 8) it.remove();
        }
    }

    /** 世界阶段画虚形。真身被 Pre 取消后，这条路径仍然会跑。 */
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (Player player : mc.level.players()) {
            if (!ClientEvents.isThunderAccelActive(player.getUUID())) continue;
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (!(player instanceof AbstractClientPlayer clientPlayer)) continue;
            renderOne(clientPlayer, poseStack, buffer, partialTick);
        }
        poseStack.popPose();
        buffer.endBatch();
        renderWorldFilter(event, poseStack, buffer, camera);
    }

    /** 灰白滤镜贴在镜头前，只罩世界，不盖血条和物品栏。 */
    private static void renderWorldFilter(RenderLevelStageEvent event, PoseStack poseStack,
                                          MultiBufferSource.BufferSource buffer, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientEvents.isThunderAccelActive(mc.player.getUUID())) return;
        Vec3 look = Vec3.directionFromRotation(event.getCamera().getXRot(), event.getCamera().getYRot());
        Vec3 center = camera.add(look.scale(0.35D));
        poseStack.pushPose();
        poseStack.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-event.getCamera().getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(event.getCamera().getXRot()));
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugFilledBox());
        float half = 2.4F;
        consumer.vertex(poseStack.last().pose(), -half, -half, 0.0F).color(216, 216, 216, 90).endVertex();
        consumer.vertex(poseStack.last().pose(), -half, half, 0.0F).color(216, 216, 216, 90).endVertex();
        consumer.vertex(poseStack.last().pose(), half, half, 0.0F).color(216, 216, 216, 90).endVertex();
        consumer.vertex(poseStack.last().pose(), half, -half, 0.0F).color(216, 216, 216, 90).endVertex();
        poseStack.popPose();
        buffer.endBatch(RenderType.debugFilledBox());
    }

    private static void renderOne(AbstractClientPlayer player, PoseStack poseStack,
                                  MultiBufferSource buffer, float partialTick) {
        Deque<Sample> trail = TRAILS.get(player.getUUID());
        if (trail == null || trail.isEmpty()) return;
        int now = player.tickCount;
        Sample delayed = null;
        for (Sample sample : trail) {
            if (now - sample.tick >= DELAY_TICKS) delayed = sample;
        }
        if (delayed == null) delayed = trail.peekFirst();
        if (delayed == null) return;

        float bob = Mth.sin((now + partialTick) * 0.45F) * 0.06F;
        poseStack.pushPose();
        poseStack.translate(delayed.x, delayed.y + bob, delayed.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - delayed.bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        PlayerModel<AbstractClientPlayer> model = modelFor(player);
        applyPose(model, delayed);
        int block = player.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, player.blockPosition());
        int sky = player.level().getBrightness(net.minecraft.world.level.LightLayer.SKY, player.blockPosition());
        int light = LightTexture.pack(block, sky);
        var consumer = buffer.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(
                player.getSkinTextureLocation()));
        model.renderToBuffer(poseStack, consumer, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                0.08F, 0.08F, 0.08F, 0.62F);
        poseStack.popPose();
    }

    private static PlayerModel<AbstractClientPlayer> modelFor(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        boolean slim = "slim".equals(player.getModelName());
        if (slim) {
            if (slimModel == null) {
                slimModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            }
            return slimModel;
        }
        if (wideModel == null) {
            wideModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return wideModel;
    }

    private static void applyPose(PlayerModel<?> model, Sample sample) {
        model.attackTime = sample.attackTime;
        model.riding = sample.riding;
        model.young = false;
        model.head.xRot = sample.headPitch * Mth.DEG_TO_RAD;
        model.head.yRot = Mth.wrapDegrees(sample.headYaw - sample.bodyYaw) * Mth.DEG_TO_RAD;
        model.hat.copyFrom(model.head);
        float swing = sample.limbSwing;
        float amount = Math.max(0.35F, sample.limbSwingAmount);
        model.rightArm.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 2.0F * amount * 0.5F;
        model.leftArm.xRot = Mth.cos(swing * 0.6662F) * 2.0F * amount * 0.5F;
        model.rightLeg.xRot = Mth.cos(swing * 0.6662F) * 1.4F * amount;
        model.leftLeg.xRot = Mth.cos(swing * 0.6662F + Mth.PI) * 1.4F * amount;
        model.body.xRot = 0.0F;
        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.zRot = 0.0F;
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private record Sample(int tick, double x, double y, double z, float bodyYaw, float headYaw,
                          float headPitch, float limbSwing, float limbSwingAmount,
                          float attackTime, boolean riding) {
        static Sample capture(Player player) {
            return new Sample(player.tickCount, player.getX(), player.getY(), player.getZ(),
                    player.yBodyRot, player.yHeadRot, player.getXRot(),
                    player.walkAnimation.position(), player.walkAnimation.speed(),
                    player.attackAnim, player.isPassenger());
        }
    }
}
