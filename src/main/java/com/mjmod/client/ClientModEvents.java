package com.mjmod.client;

import com.mjmod.MJMod;
import com.mjmod.client.model.VoidTearsHelmetModel;
import com.mjmod.client.model.VoidTearBeamModel;
import com.mjmod.client.model.VoidTearBladeModel;
import com.mjmod.client.renderer.VoidTearsHelmetLayer;
import com.mjmod.client.renderer.VoidTearBeamRenderer;
import com.mjmod.client.renderer.VoidTearShieldRenderer;
import com.mjmod.client.renderer.VoidTearShotRenderer;
import com.mjmod.client.renderer.VoidTearBladeRenderer;
import com.mjmod.client.model.VoidTearShieldModel;
import com.mjmod.client.model.VoidTearShotModel;
import com.mjmod.client.model.VoidTearCastAnimationModel;
import com.mjmod.client.model.VoidTearDropModel;
import com.mjmod.client.renderer.VoidTearDropRenderer;
import com.mjmod.client.model.EHuiShouModel;
import com.mjmod.client.renderer.EHuiShouRenderer;
import com.mjmod.client.model.ShenQingShuiZhiYaoModel;
import com.mjmod.client.model.OrientalClanModel;
import com.mjmod.client.renderer.ShenQingShuiZhiYaoRenderer;
import com.mjmod.client.renderer.OrientalClanRenderer;
import com.mjmod.client.renderer.PureFireVortexRenderer;
import com.mjmod.client.model.PureFireFistModel;
import com.mjmod.client.model.PureFireHandsModel;
import com.mjmod.client.renderer.PureFireHandsLayer;
import com.mjmod.client.renderer.PureFirePunchRenderer;
import com.mjmod.client.renderer.PureFireMeteorDiveRenderer;
import com.mjmod.client.renderer.PureFireCleaveRenderer;
import com.mjmod.client.renderer.PureFireThrustRenderer;
import com.mjmod.client.renderer.PureFireDomainRenderer;
import com.mjmod.client.model.PureFireBladeModel;
import com.mjmod.client.particle.PureFireParticle;
import com.mjmod.client.particle.PureFireAuraParticle;
import com.mjmod.client.particle.PureFireAuraCoreParticle;

import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModParticles;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端模组事件处理器（注册到 MOD 事件总线）
 */
@Mod.EventBusSubscriber(modid = MJMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.VOID_TEARS_SWITCH);
        event.register(ModKeyBindings.VOID_TEARS_CAST);
        event.register(ModKeyBindings.BLOOD_EXCHANGE);
        event.register(ModKeyBindings.OPEN_VOID_TEARS_POINTS);
        event.register(ModKeyBindings.OPEN_VOID_TEARS_HEART);
        event.register(ModKeyBindings.PURE_FIRE_SWITCH);
        event.register(ModKeyBindings.PURE_FIRE_CAST);
        event.register(ModKeyBindings.PURE_FIRE_MEDITATE);
        event.register(ModKeyBindings.OPEN_PURE_FIRE_TALENT);
        event.register(ModKeyBindings.THUNDER_ACCEL);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.PURE_FIRE.get(), PureFireParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PURE_FIRE_AURA.get(), PureFireAuraParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PURE_FIRE_AURA_CORE.get(), PureFireAuraCoreParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VOID_TEAR_PROJECTILE.get(), VoidTearBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TEAR_SHIELD.get(), VoidTearShieldRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TEAR_SHOT.get(), VoidTearShotRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TEAR_BLADE.get(), VoidTearBladeRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_TEAR_DROP.get(), VoidTearDropRenderer::new);
        event.registerEntityRenderer(ModEntities.EHUI_SHOU.get(), EHuiShouRenderer::new);
        event.registerEntityRenderer(ModEntities.SHEN_QING_SHUI_ZHI_YAO.get(),
                ShenQingShuiZhiYaoRenderer::new);
        event.registerEntityRenderer(ModEntities.ORIENTAL_CLAN.get(), OrientalClanRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_VORTEX.get(), PureFireVortexRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_PUNCH.get(), PureFirePunchRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_METEOR_DIVE.get(),
                PureFireMeteorDiveRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_CLEAVE.get(),
                PureFireCleaveRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_THRUST.get(),
                PureFireThrustRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_WALL.get(),
                com.mjmod.client.renderer.PureFireWallRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_WALL_CAST.get(),
                com.mjmod.client.renderer.PureFireWallCastRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_BALL.get(),
                com.mjmod.client.renderer.PureFireBallRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_BALL_CAST.get(),
                com.mjmod.client.renderer.PureFireBallCastRenderer::new);
        event.registerEntityRenderer(ModEntities.PURE_FIRE_DOMAIN.get(),
                PureFireDomainRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VoidTearsHelmetModel.LAYER_LOCATION, VoidTearsHelmetModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearBeamModel.LAYER_LOCATION, VoidTearBeamModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearBladeModel.LAYER_LOCATION, VoidTearBladeModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearShieldModel.LAYER_LOCATION, VoidTearShieldModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearShotModel.LAYER_LOCATION, VoidTearShotModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearCastAnimationModel.LAYER_LOCATION,
                VoidTearCastAnimationModel::createBodyLayer);
        event.registerLayerDefinition(VoidTearDropModel.LAYER_LOCATION,
                VoidTearDropModel::createBodyLayer);
        event.registerLayerDefinition(EHuiShouModel.LAYER_LOCATION,
                EHuiShouModel::createBodyLayer);
        event.registerLayerDefinition(ShenQingShuiZhiYaoModel.LAYER_LOCATION,
                ShenQingShuiZhiYaoModel::createBodyLayer);
        event.registerLayerDefinition(PureFireHandsModel.LAYER_LOCATION,
                PureFireHandsModel::createBodyLayer);
        event.registerLayerDefinition(PureFireFistModel.LAYER_LOCATION,
                PureFireFistModel::createBodyLayer);
        event.registerLayerDefinition(com.mjmod.client.model.PureFireGauntletModel.LAYER_LOCATION,
                com.mjmod.client.model.PureFireGauntletModel::createBodyLayer);
        event.registerLayerDefinition(PureFireBladeModel.LAYER_LOCATION,
                PureFireBladeModel::createBodyLayer);
        event.registerLayerDefinition(com.mjmod.client.model.PureFireBallModel.LAYER_LOCATION,
                com.mjmod.client.model.PureFireBallModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            renderer.addLayer(new VoidTearsHelmetLayer(renderer, event.getEntityModels()));
            renderer.addLayer(new PureFireHandsLayer(renderer, event.getEntityModels()));

        }
    }
}
