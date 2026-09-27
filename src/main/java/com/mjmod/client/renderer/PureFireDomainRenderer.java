package com.mjmod.client.renderer;

import com.mjmod.MJMod;
import com.mjmod.entity.PureFireDomainEntity;
import com.mjmod.skill.PureFireDomainGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阳炎领域的实体渲染器只保留实体注册所需的空渲染入口；真正的绘制在
 * AFTER_TRANSLUCENT_BLOCKS 阶段完成，避免把世界网格绑定到实体模型坐标。
 *
 * <p>从中心向外渐进扫描真实方块的 VoxelShape。只描绘有露出面的方块，
 * 地面、台阶、建筑和屋檐下方共用同一套形状边缘，不以高度图虚构悬空地表。
 *
 * <p>描边颜色保持金红两色不变；边界立方体与真实地形边缘共用三层加法光晕，
 * 方块描边带低强度光带，蔓延火环经过时骤亮。每 600 ticks 自动重扫一次
 * 以跟进地形改动，重扫期间继续显示旧网格。
 */
@Mod.EventBusSubscriber(modid = MJMod.MODID, value = Dist.CLIENT)
public final class PureFireDomainRenderer extends EntityRenderer<PureFireDomainEntity> {
    /** 150×150 领域对应方块偏移 -75..+74。 */
    private static final int HALF = PureFireDomainGeometry.HALF_SIZE;

    /** 开局蔓延：描边从中心长到满域所用 tick 数。 */
    private static final float GROW_TICKS = 40.0F;
    /** 蔓延火环的循环周期。 */
    private static final float WAVE_PERIOD_TICKS = 60.0F;
    /** 火环从中心跑到边界所用 tick 数（小于周期，留出间歇）。 */
    private static final float WAVE_TRAVEL_TICKS = 45.0F;
    /** 火环途经时点亮方块描边的环带宽度。 */
    private static final float WAVE_BAND = 2.0F;
    /** 全量重扫间隔（跟进地形改动）。 */
    private static final long RESCAN_INTERVAL_TICKS = 600L;
    /** 每帧扫描 96 列；每列最多 61 个 Y 层，初次建网格分帧完成。 */
    private static final int SCAN_COLUMNS_PER_FRAME = 96;
    /** 线段缓存上限，防止崎岖地形把每帧顶点量推到不可控。 */
    private static final int MAX_CACHED_SEGMENTS = 80000;
    /** 近距离辉光顶点上限；远处仍保留轻量线框。 */
    private static final int MAX_GLOW_SEGMENTS_PER_FRAME = 16000;
    /** 方块描边光晕层的半宽。 */
    private static final float GRID_GLOW_WIDTH = 0.60F;
    /** 光晕只给相机该距离内的描边段绘制（远处线框已足够密）。 */
    private static final float GLOW_LOD_DISTANCE_SQ = 96.0F * 96.0F;
    /** 边界立方体与真实地形边缘光晕三层嵌套的半宽与基础透明度。 */
    private static final float[] GLOW_WIDTHS = {0.20F, 0.55F, 1.10F};
    private static final float[] GLOW_ALPHAS = {0.50F, 0.22F, 0.10F};

    private static final Direction[] EXPOSED_DIRS = Direction.values();

    private static final Map<UUID, DomainViews> EDGE_CACHE = new ConcurrentHashMap<>();

    /** 缓存附近已追踪的领域实体，每游戏 tick 更新。 */
    private static List<PureFireDomainEntity> domainCache = List.of();
    private static long domainCacheStamp = Long.MIN_VALUE;
    private static Level cachedLevel;
    private static double domainCacheX;
    private static double domainCacheY;
    private static double domainCacheZ;

    private static final class DomainViews {
        /** 已完成扫描的缓存，渲染优先用它；首次构建完成前为 null。 */
        EdgeCache current;
        /** 进行中的扫描任务，null 表示当前无扫描。 */
        EdgeBuilder pending;
        int scanRing;
        int scanStep;
    }

    private static final class EdgeCache {
        /** 每段 8 个浮点：x1,y1,z1,x2,y2,z2,ring(切比雪夫环距),vertical。 */
        float[] data;
        int segments;
        long builtAtGameTime;
        double cx;
        double cy;
        double cz;
    }

    private static final class EdgeBuilder {
        float[] data = new float[8192];
        int segments;

        void add(float x1, float y1, float z1, float x2, float y2, float z2,
                 float ring, float vertical) {
            if ((segments + 1) * 8 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            int o = segments * 8;
            data[o] = x1;
            data[o + 1] = y1;
            data[o + 2] = z1;
            data[o + 3] = x2;
            data[o + 4] = y2;
            data[o + 5] = z2;
            data[o + 6] = ring;
            data[o + 7] = vertical;
            segments++;
        }
    }

    public PureFireDomainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (cachedLevel != mc.level) {
            EDGE_CACHE.clear();
            domainCache = List.of();
            domainCacheStamp = Long.MIN_VALUE;
            cachedLevel = mc.level;
        }

        Vec3 camera = event.getCamera().getPosition();
        for (PureFireDomainEntity domain : findDomains(mc, camera)) {
            if (!domain.isRemoved()) renderDomain(domain, event, camera);
        }
        EDGE_CACHE.keySet().removeIf(id -> domainCache.stream().noneMatch(d -> d.getUUID().equals(id)));
    }

    private static List<PureFireDomainEntity> findDomains(Minecraft mc, Vec3 camera) {
        long now = mc.level.getGameTime();
        double dx = camera.x - domainCacheX;
        double dy = camera.y - domainCacheY;
        double dz = camera.z - domainCacheZ;
        boolean moved = dx * dx + dy * dy + dz * dz > 48.0D * 48.0D;
        if (now != domainCacheStamp || moved) {
            domainCache = mc.level.getEntitiesOfClass(PureFireDomainEntity.class,
                    new AABB(camera.x - 256.0D, camera.y - 256.0D, camera.z - 256.0D,
                            camera.x + 256.0D, camera.y + 256.0D, camera.z + 256.0D),
                    entity -> !entity.isRemoved());
            domainCacheStamp = now;
            domainCacheX = camera.x;
            domainCacheY = camera.y;
            domainCacheZ = camera.z;
        }
        return domainCache;
    }

    private static void renderDomain(PureFireDomainEntity domain,
                                     RenderLevelStageEvent event, Vec3 camera) {
        float age = domain.tickCount + event.getPartialTick();
        float growT = Mth.clamp(age / GROW_TICKS, 0.0F, 1.0F);
        float growEase = 1.0F - (1.0F - growT) * (1.0F - growT) * (1.0F - growT);
        double currentHalf = PureFireDomainGeometry.growthRadius(age);

        // 蔓延波前：开局跟随生长前沿；之后周期性从中心奔向边界
        float waveR = -1.0F;
        if (age < GROW_TICKS) {
            waveR = (float) currentHalf;
        } else {
            float phase = (age - GROW_TICKS) % WAVE_PERIOD_TICKS;
            if (phase < WAVE_TRAVEL_TICKS) {
                float p = phase / WAVE_TRAVEL_TICKS;
                p = 1.0F - (1.0F - p) * (1.0F - p);
                waveR = (float) (p * PureFireDomainEntity.HORIZONTAL_HALF_SIZE);
            }
        }

        float pulse = 0.78F + 0.22F * Mth.sin(age * 0.12F);
        EdgeCache view = edgeCache(domain);
        MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // 描边层：边界立方体 + 真实方块网格描边
        VertexConsumer lines = source.getBuffer(RenderType.lines());
        drawBoundary(poseStack, lines, domain, pulse, growEase, currentHalf);
        if (view != null) drawGridLines(poseStack, lines, view, pulse, growEase, currentHalf, waveR);

        // 发光层：加法混合光晕（getBuffer 切换时上一批 lines 会自动提交）
        VertexConsumer glow = source.getBuffer(RenderType.lightning());
        drawBoundaryGlow(poseStack, glow, domain, camera, age, growEase, currentHalf);
        if (view != null) drawGridGlow(poseStack, glow, camera, view, age, growEase, currentHalf, waveR);
        if (waveR > 1.0F) {
            drawWaveRing(poseStack, glow, domain, waveR);
        }
        source.endBatch(RenderType.lightning());
        source.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    private static EdgeCache edgeCache(PureFireDomainEntity domain) {
        long now = domain.level().getGameTime();
        DomainViews views = EDGE_CACHE.computeIfAbsent(domain.getUUID(), id -> new DomainViews());
        boolean moved = views.current != null && (
                Math.abs(views.current.cx - domain.getX()) > 1.0E-3D
                        || Math.abs(views.current.cy - domain.getY()) > 1.0E-3D
                        || Math.abs(views.current.cz - domain.getZ()) > 1.0E-3D);
        if (views.pending == null) {
            if (views.current == null || moved
                    || now - views.current.builtAtGameTime >= RESCAN_INTERVAL_TICKS) {
                startScan(views);
            }
        }
        if (views.pending != null) {
            runScan(domain, views, now);
        }
        // 重扫期间继续显示旧网格；首次构建时直接显示扫描中的部分结果（随蔓延铺开）
        if (views.pending != null && views.current == null) {
            EdgeCache partial = new EdgeCache();
            partial.data = views.pending.data;
            partial.segments = views.pending.segments;
            partial.builtAtGameTime = now;
            partial.cx = domain.getX();
            partial.cy = domain.getY();
            partial.cz = domain.getZ();
            return partial;
        }
        return views.current;
    }

    /** 开启一次真实 VoxelShape 扫描。 */
    private static void startScan(DomainViews views) {
        views.pending = new EdgeBuilder();
        views.scanRing = 0;
        views.scanStep = 0;
    }

    private static void runScan(PureFireDomainEntity domain, DomainViews views, long now) {
        Level level = domain.level();
        BlockPos center = BlockPos.containing(domain.getX(), domain.getY(), domain.getZ());
        float bottomY = (float) (domain.getY() + PureFireDomainEntity.MIN_Y_OFFSET);
        float topY = (float) (domain.getY() + PureFireDomainEntity.MAX_Y_OFFSET);
        int y0 = PureFireDomainEntity.MIN_Y_OFFSET;
        int budget = SCAN_COLUMNS_PER_FRAME;

        while (budget > 0) {
            int r = views.scanRing;
            if (r > HALF || views.pending.segments >= MAX_CACHED_SEGMENTS) {
                finishScan(domain, views, now);
                return;
            }
            int cells = r == 0 ? 1 : 8 * r;
            while (views.scanStep < cells && budget > 0) {
                int ox;
                int oz;
                if (r == 0) {
                    ox = 0;
                    oz = 0;
                } else {
                    int side = views.scanStep / (2 * r);
                    int off = views.scanStep % (2 * r);
                    switch (side) {
                        case 0 -> { ox = -r + off; oz = -r; }
                        case 1 -> { ox = r; oz = -r + off; }
                        case 2 -> { ox = r - off; oz = r; }
                        default -> { ox = -r; oz = r - off; }
                    }
                }
                BlockPos columnBase = center.offset(ox, y0, oz);
                if (level.hasChunkAt(columnBase)) {
                    scanColumn(domain, level, center, ox, oz, y0, bottomY, topY, views.pending, r);
                }
                budget--;
                views.scanStep++;
            }
            if (views.scanStep >= cells) {
                views.scanRing++;
                views.scanStep = 0;
            }
        }
    }

    private static void finishScan(PureFireDomainEntity domain, DomainViews views, long now) {
        EdgeCache cache = new EdgeCache();
        cache.data = views.pending.data;
        cache.segments = views.pending.segments;
        cache.builtAtGameTime = now;
        cache.cx = domain.getX();
        cache.cy = domain.getY();
        cache.cz = domain.getZ();
        views.current = cache;
        views.pending = null;
    }

    /** 有顶面、侧面或底面露出的方块都描绘实际形状。 */
    private static void scanColumn(PureFireDomainEntity domain, Level level, BlockPos center,
                                   int ox, int oz, int y0, float bottomY, float topY,
                                   EdgeBuilder builder, int ring) {
        float ringF = Math.min(HALF, ring);
        for (int dy = y0; dy <= PureFireDomainEntity.MAX_Y_OFFSET; dy++) {
            BlockPos pos = center.offset(ox, dy, oz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getShape(level, pos).isEmpty()) continue;
            boolean exposed = false;
            for (Direction direction : EXPOSED_DIRS) {
                BlockPos adjacent = pos.relative(direction);
                if (!level.getBlockState(adjacent).isSolidRender(level, adjacent)) {
                    exposed = true;
                    break;
                }
            }
            if (!exposed) continue;
            emitBlockEdges(builder, level, pos, state, ox, oz, bottomY, topY, ringF);
        }
    }

    /** 有露出面的方块按真实形状描边；台阶与半砖不使用整方块假线框。 */
    private static void emitBlockEdges(EdgeBuilder builder, Level level, BlockPos pos,
                                       BlockState state, int ox, int oz,
                                       float bottomY, float topY, float ring) {
        state.getShape(level, pos).forAllEdges((ax, ay, az, bx, by, bz) -> {
            if (builder.segments >= MAX_CACHED_SEGMENTS) return;
            float ey1 = (float) (pos.getY() + ay);
            float ey2 = (float) (pos.getY() + by);
            float midY = (ey1 + ey2) * 0.5F;
            if (midY < bottomY || midY > topY) return;
            builder.add((float) (pos.getX() + ax), ey1, (float) (pos.getZ() + az),
                    (float) (pos.getX() + bx), ey2, (float) (pos.getZ() + bz),
                    ring, Math.abs(by - ay) > 0.5F ? 1.0F : 0.0F);
        });
    }


    private static void drawBoundary(PoseStack poseStack, VertexConsumer lines,
                                     PureFireDomainEntity domain, float alpha,
                                     float growEase, double currentHalf) {
        float x0 = (float) (domain.getX() - currentHalf);
        float x1 = (float) (domain.getX() + currentHalf);
        float z0 = (float) (domain.getZ() - currentHalf);
        float z1 = (float) (domain.getZ() + currentHalf);
        float y0 = (float) (domain.getY() + PureFireDomainEntity.MIN_Y_OFFSET * growEase);
        float y1 = (float) (domain.getY() + PureFireDomainEntity.MAX_Y_OFFSET * growEase);
        float edgeAlpha = Mth.clamp(alpha * Math.max(0.25F, growEase), 0.0F, 1.0F);

        drawLine(poseStack, lines, x0, y0, z0, x0, y1, z0, 1.0F, 0.10F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x1, y0, z0, x1, y1, z0, 1.0F, 0.10F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y0, z1, x0, y1, z1, 1.0F, 0.10F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x1, y0, z1, x1, y1, z1, 1.0F, 0.10F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y0, z0, x1, y0, z0, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y0, z1, x1, y0, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y1, z0, x1, y1, z0, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y1, z1, x1, y1, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y0, z0, x0, y0, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x1, y0, z0, x1, y0, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x0, y1, z0, x0, y1, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);
        drawLine(poseStack, lines, x1, y1, z0, x1, y1, z1, 1.0F, 0.72F, 0.02F, edgeAlpha);

        // 外圈疏疏落落的立柱组成透明火幕；比整面实体墙更不挡第一人称视野。
        for (int step = 1; step < 9; step++) {
            float t = step / 9.0F;
            float bx = Mth.lerp(t, x0, x1);
            float bz = Mth.lerp(t, z0, z1);
            float a = edgeAlpha * (step % 2 == 0 ? 0.26F : 0.13F);
            drawLine(poseStack, lines, bx, y0, z0, bx, y1, z0, 1.0F, 0.14F, 0.02F, a);
            drawLine(poseStack, lines, bx, y0, z1, bx, y1, z1, 1.0F, 0.14F, 0.02F, a);
            drawLine(poseStack, lines, x0, y0, bz, x0, y1, bz, 1.0F, 0.14F, 0.02F, a);
            drawLine(poseStack, lines, x1, y0, bz, x1, y1, bz, 1.0F, 0.14F, 0.02F, a);
        }
    }


    /** 地表方块网格描边：按蔓延门控与波前亮度逐帧重算颜色。 */
    private static void drawGridLines(PoseStack poseStack, VertexConsumer lines,
                                      EdgeCache cache, float pulse, float growEase,
                                      double currentHalf, float waveR) {
        float[] data = cache.data;
        for (int i = 0; i < cache.segments; i++) {
            int o = i * 8;
            float ring = data[o + 6];
            if (ring > currentHalf + 0.45F) continue;
            float boost = waveR >= 0.0F
                    ? (float) Math.max(0.0D, 1.0D - Math.abs(ring - waveR) / WAVE_BAND) * 0.45F
                    : 0.0F;
            float alpha = Mth.clamp(pulse * 0.9F + boost, 0.0F, 1.0F);
            if (data[o + 7] > 0.5F) {
                drawLine(poseStack, lines, data[o], data[o + 1], data[o + 2],
                        data[o + 3], data[o + 4], data[o + 5], 1.0F, 0.12F, 0.02F, alpha);
            } else {
                drawLine(poseStack, lines, data[o], data[o + 1], data[o + 2],
                        data[o + 3], data[o + 4], data[o + 5], 1.0F, 0.72F, 0.02F, alpha);
            }
        }
    }

    /**
     * 方块描边的发光层：水平段画贴地光带，竖直段画朝向相机的光带，
     * 基础低强度常亮 + 波前经过时骤亮；远处描边段只画线框不画光晕。
     */
    private static void drawGridGlow(PoseStack poseStack, VertexConsumer glow, Vec3 camera,
                                     EdgeCache cache, float age, float growEase,
                                     double currentHalf, float waveR) {
        if (growEase <= 0.02F) return;
        float strength = growEase * (0.80F + 0.20F * Mth.sin(age * 0.18F));
        float[] data = cache.data;
        PoseStack.Pose pose = poseStack.last();
        int emitted = 0;
        for (int i = 0; i < cache.segments; i++) {
            if (emitted >= MAX_GLOW_SEGMENTS_PER_FRAME) break;
            int o = i * 8;
            float ring = data[o + 6];
            if (ring > currentHalf + 0.45F) continue;
            float boost = waveR >= 0.0F
                    ? (float) Math.max(0.0D, 1.0D - Math.abs(ring - waveR) / WAVE_BAND) * 0.55F
                    : 0.0F;
            float glowAlpha = 0.26F * strength + boost;
            if (glowAlpha <= 0.004F) continue;
            float x1 = data[o];
            float y1 = data[o + 1];
            float z1 = data[o + 2];
            float x2 = data[o + 3];
            float y2 = data[o + 4];
            float z2 = data[o + 5];
            float mx = (x1 + x2) * 0.5F - (float) camera.x;
            float my = (y1 + y2) * 0.5F - (float) camera.y;
            float mz = (z1 + z2) * 0.5F - (float) camera.z;
            if (mx * mx + my * my + mz * mz > GLOW_LOD_DISTANCE_SQ) continue;
            if (data[o + 7] > 0.5F) {
                drawBillboardQuad(glow, pose, x1, y1, z1, x2, y2, z2,
                        camera, GRID_GLOW_WIDTH, 1.0F, 0.30F, 0.05F, glowAlpha);
            } else {
                float dx = x2 - x1;
                float dz = z2 - z1;
                float len = Mth.sqrt(dx * dx + dz * dz);
                if (len < 1.0E-4F) continue;
                float px = -dz / len * GRID_GLOW_WIDTH;
                float pz = dx / len * GRID_GLOW_WIDTH;
                drawQuad(glow, pose,
                        x1 - px, y1, z1 - pz, x1 + px, y1, z1 + pz,
                        x2 + px, y2, z2 + pz, x2 - px, y2, z2 - pz,
                        1.0F, 0.62F, 0.10F, glowAlpha);
            }
            emitted++;
        }
    }

    /**
     * 边界立方体 12 条边的发光层：每条边生成一个始终朝向相机的广告牌面片，
     * 三层嵌套加法混合形成中心亮、边缘淡的辉光。
     */
    private static void drawBoundaryGlow(PoseStack poseStack, VertexConsumer glow,
                                         PureFireDomainEntity domain, Vec3 camera,
                                         float age, float growEase, double currentHalf) {
        if (growEase <= 0.02F) return;
        float strength = growEase * (0.80F + 0.20F * Mth.sin(age * 0.18F));

        float x0 = (float) (domain.getX() - currentHalf);
        float x1 = (float) (domain.getX() + currentHalf);
        float z0 = (float) (domain.getZ() - currentHalf);
        float z1 = (float) (domain.getZ() + currentHalf);
        float y0 = (float) (domain.getY() + PureFireDomainEntity.MIN_Y_OFFSET * growEase);
        float y1 = (float) (domain.getY() + PureFireDomainEntity.MAX_Y_OFFSET * growEase);

        float[][] edges = {
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0},
                {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1},
                {x0, y1, z0, x1, y1, z0}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1},
                {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1},
        };
        PoseStack.Pose pose = poseStack.last();
        for (float[] e : edges) {
            boolean vertical = Math.abs(e[3] - e[0]) + Math.abs(e[5] - e[2]) < 1.0E-6F;
            float r = 1.0F;
            float g = vertical ? 0.30F : 0.62F;
            float b = vertical ? 0.04F : 0.08F;
            for (int layer = 0; layer < GLOW_WIDTHS.length; layer++) {
                float a = GLOW_ALPHAS[layer] * strength;
                if (a <= 0.004F) continue;
                drawBillboardQuad(glow, pose, e[0], e[1], e[2], e[3], e[4], e[5],
                        camera, GLOW_WIDTHS[layer], r, g, b, a);
            }
        }
    }


    /** 单层相机朝向光带：side = normalize(边方向 × 视线方向)。 */
    private static void drawBillboardQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                          float x1, float y1, float z1,
                                          float x2, float y2, float z2,
                                          Vec3 camera, float width,
                                          float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) return;
        dx /= len;
        dy /= len;
        dz /= len;

        float mx = (x1 + x2) * 0.5F - (float) camera.x;
        float my = (y1 + y2) * 0.5F - (float) camera.y;
        float mz = (z1 + z2) * 0.5F - (float) camera.z;
        float viewLen = Mth.sqrt(mx * mx + my * my + mz * mz);
        if (viewLen < 1.0E-4F) return;
        mx /= viewLen;
        my /= viewLen;
        mz /= viewLen;

        float sx = dy * mz - dz * my;
        float sy = dz * mx - dx * mz;
        float sz = dx * my - dy * mx;
        float sideLen = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sideLen < 1.0E-4F) {
            // 视线与边几乎共线（如竖直边俯视）：改用边的水平法线兜底
            sx = -dz;
            sy = 0.0F;
            sz = dx;
            sideLen = Mth.sqrt(sx * sx + sz * sz);
            if (sideLen < 1.0E-4F) {
                sx = 1.0F;
                sz = 0.0F;
                sideLen = 1.0F;
            }
        }
        sx /= sideLen;
        sy /= sideLen;
        sz /= sideLen;

        drawQuad(consumer, pose,
                x1 - sx * width, y1 - sy * width, z1 - sz * width,
                x1 + sx * width, y1 + sy * width, z1 + sz * width,
                x2 + sx * width, y2 + sy * width, z2 + sz * width,
                x2 - sx * width, y2 - sy * width, z2 - sz * width,
                r, g, b, a);
    }

    /** 蔓延火环：地面上以波前为边长的加法混合正方形光环。 */
    private static void drawWaveRing(PoseStack poseStack, VertexConsumer glow,
                                     PureFireDomainEntity domain, float ringR) {
        float cx = (float) domain.getX();
        float cz = (float) domain.getZ();
        float y = (float) (domain.getY() + 0.15D);
        float half = ringR;
        float progress = ringR / (float) PureFireDomainEntity.HORIZONTAL_HALF_SIZE;
        float alpha = 0.34F * (1.0F - 0.45F * progress);
        if (progress > 0.86F) {
            alpha *= Math.max(0.0F, (1.0F - progress) / 0.14F);
        }
        float w = 0.55F;

        PoseStack.Pose pose = poseStack.last();
        drawQuad(glow, pose,
                cx - half, y, cz - half - w, cx + half, y, cz - half - w,
                cx + half, y, cz - half + w, cx - half, y, cz - half + w,
                1.0F, 0.55F, 0.10F, alpha);
        drawQuad(glow, pose,
                cx - half, y, cz + half - w, cx + half, y, cz + half - w,
                cx + half, y, cz + half + w, cx - half, y, cz + half + w,
                1.0F, 0.55F, 0.10F, alpha);
        drawQuad(glow, pose,
                cx - half - w, y, cz - half, cx - half - w, y, cz + half,
                cx - half + w, y, cz + half, cx - half + w, y, cz - half,
                1.0F, 0.55F, 0.10F, alpha);
        drawQuad(glow, pose,
                cx + half - w, y, cz - half, cx + half - w, y, cz + half,
                cx + half + w, y, cz + half, cx + half + w, y, cz - half,
                1.0F, 0.55F, 0.10F, alpha);
    }

    private static void drawQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 float ax, float ay, float az,
                                 float bx, float by, float bz,
                                 float cx, float cy, float cz,
                                 float dx, float dy, float dz,
                                 float r, float g, float b, float a) {
        consumer.vertex(pose.pose(), ax, ay, az).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), bx, by, bz).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), cx, cy, cz).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), dx, dy, dz).color(r, g, b, a).endVertex();
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = length > 1.0E-4F ? dx / length : 0.0F;
        float ny = length > 1.0E-4F ? dy / length : 1.0F;
        float nz = length > 1.0E-4F ? dz / length : 0.0F;
        consumer.vertex(pose.pose(), x1, y1, z1)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
        consumer.vertex(pose.pose(), x2, y2, z2)
                .color(red, green, blue, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRender(PureFireDomainEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(PureFireDomainEntity entity) {
        return new ResourceLocation(MJMod.MODID, "textures/gui/pure_fire/skill_10.png");
    }
}
