package com.mjmod;

import com.mjmod.entity.EHuiShouEntity;
import com.mjmod.entity.VoidTearShieldEntity;
import com.mjmod.item.QianLiYuanXingFuItem;
import com.mjmod.network.MJNetwork;
import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModItems;
import com.mjmod.registry.ModParticles;
import com.mjmod.registry.ModSounds;
import com.mjmod.skill.BloodExchangeManager;
import com.mjmod.skill.RelationshipManager;
import com.mjmod.skill.VoidTearsSkillManager;
import com.mjmod.skill.PureFireSkillManager;
import com.mjmod.skill.ThunderAccelSkillManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Main mod entry point. */
@Mod(MJMod.MODID)
public class MJMod {
    public static final String MODID = "mjmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MJMod() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(MJMod::onAttributeCreate);
        modEventBus.addListener(MJMod::onSpawnPlacementRegister);
        MinecraftForge.EVENT_BUS.register(this);
        MJNetwork.register();
        LOGGER.info("[MJ] 狐妖小红娘 Mod 加载成功！");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (event.getRawText() != null && event.getRawText().trim().equalsIgnoreCase("mj")) {
            event.setCanceled(true);
            ServerPlayer player = event.getPlayer();
            MJNetwork.sendOpenScreen(player);
            player.sendSystemMessage(Component.literal("§a[MJ] §7素材已打开，按 ESC 关闭。"));
        }
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            if (player.tickCount % 20 == 0) {
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    QianLiYuanXingFuItem.clearLegacyMobBinding(
                            player.getInventory().getItem(slot));
                }
            }
            VoidTearsSkillManager.migrateLegacyEquippedItem(player);
            VoidTearsSkillManager.tickActivation(player);
            VoidTearsSkillManager.tickTearDropAutomation(player);
            PureFireSkillManager.tick(player);
            ThunderAccelSkillManager.tick(player);
            RelationshipManager.ensureAppearance(player);
            BloodExchangeManager.tick(player);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() != null
                && ThunderAccelSkillManager.shouldCancelIncomingHit(
                        event.getEntity(), event.getSource().getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getEntity() != null
                && event.getSource().getEntity() != victim
                && victim.getRandom().nextFloat() < 0.10F) {
            ItemStack hair = new ItemStack(ModItems.PLAYER_HAIR.get());
            QianLiYuanXingFuItem.bindPlayerHair(hair, victim);
            victim.spawnAtLocation(hair, 0.35F);
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            float adjusted = PureFireSkillManager.modifyIncomingDamage(victim, event.getAmount());
            if (adjusted < event.getAmount() - 1.0E-4F || adjusted > event.getAmount() + 1.0E-4F) {
                event.setAmount(adjusted);
            }
            VoidTearsSkillManager.triggerTearDropDefense(victim, event.getSource().getEntity());
            float incoming = event.getAmount();
            float remaining = VoidTearShieldEntity.absorbProjectileDamage(
                    victim, event.getSource(), incoming);
            if (remaining < incoming - 1.0E-4F) {
                if (remaining <= 1.0E-4F) event.setCanceled(true);
                else event.setAmount(remaining);
            }
        }
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            PureFireSkillManager.applyBladeHoldWound(attacker, event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            PureFireSkillManager.tryLearnFromKill(player, event.getEntity());
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ThunderAccelSkillManager.tickProjectiles(serverLevel);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (ThunderAccelSkillManager.shouldFreezeTick(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mjvoidtears")
                .then(Commands.literal("heart")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(MJMod::setTragicHeartCommand)))
                        .then(Commands.literal("view").executes(MJMod::viewTragicHeartCommand))));
        event.getDispatcher().register(Commands.literal("mjleech")
                .then(Commands.literal("affection")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(MJMod::setLeechAffectionCommand)))
                        .then(Commands.literal("view").executes(MJMod::viewLeechAffectionCommand)))
                .then(Commands.literal("music")
                        .then(Commands.literal("on").executes(ctx -> setLeechMusicCommand(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setLeechMusicCommand(ctx, false)))
                        .then(Commands.literal("status").executes(MJMod::viewLeechMusicCommand))));
        event.getDispatcher().register(Commands.literal("mjblood")
                .then(Commands.literal("learn").executes(MJMod::grantBloodTechniqueCommand))
                .then(Commands.literal("permission").executes(MJMod::grantDivineBloodPermissionCommand))
                .then(Commands.literal("divine").executes(MJMod::grantDivineBloodPermissionCommand)));
        event.getDispatcher().register(Commands.literal("mjpurefire")
                .then(Commands.literal("permission").executes(MJMod::grantPureFirePermissionCommand))
                .then(Commands.literal("oriental").executes(MJMod::becomeOrientalClanCommand))
                .then(Commands.literal("unlock")
                        .then(Commands.literal("all").executes(MJMod::unlockAllPureFireSkillsCommand))
                        .then(Commands.argument("skill", IntegerArgumentType.integer(1, PureFireSkillManager.SKILL_COUNT))
                                .executes(MJMod::unlockPureFireSkillCommand)))
                .then(Commands.literal("talent")
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-999, 999))
                                        .executes(MJMod::addPureFireTalentCommand)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 999))
                                        .executes(MJMod::setPureFireTalentCommand)))
                        .then(Commands.literal("view").executes(MJMod::viewPureFireTalentCommand))
                        .then(Commands.literal("resetcd").executes(MJMod::resetPureFireMeditationCdCommand)))
                .then(Commands.literal("proficiency")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(MJMod::setPureFireProficiencyCommand)))
                        .then(Commands.literal("view").executes(MJMod::viewPureFireProficiencyCommand))));
        event.getDispatcher().register(Commands.literal("mjthunder")
                .then(Commands.literal("get").executes(MJMod::getThunderTechniqueCommand)));
    }

    private static ServerPlayer commandPlayer(CommandContext<CommandSourceStack> ctx) {
        try { return ctx.getSource().getPlayer(); }
        catch (Exception e) { ctx.getSource().sendFailure(Component.literal("§c该命令只能由玩家执行")); return null; }
    }

    private static int setTragicHeartCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int value = IntegerArgumentType.getInteger(ctx, "value"); VoidTearsSkillManager.set悲情之心(p, value);
        ctx.getSource().sendSuccess(() -> Component.literal("§b已将悲情之心设置为 " + value + "%"), false); return 1;
    }
    private static int viewTragicHeartCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int value = VoidTearsSkillManager.get悲情之心(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§b悲情之心：" + value + "% §7| 等级：LV" + VoidTearsSkillManager.getVoidTearsLevel(p)), false); return 1;
    }
    private static int setLeechAffectionCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int value = IntegerArgumentType.getInteger(ctx, "value"); RelationshipManager.setAffection(p, value);
        ctx.getSource().sendSuccess(() -> Component.literal("§d已将水蛭妖好感度设置为 " + value + "/100"), false); return 1;
    }
    private static int viewLeechAffectionCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        ctx.getSource().sendSuccess(() -> Component.literal("§d水蛭妖好感度：" + RelationshipManager.getAffection(p) + "/100"), false); return 1;
    }
    private static int setLeechMusicCommand(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0; RelationshipManager.setDialogueMusic(p, enabled);
        ctx.getSource().sendSuccess(() -> Component.literal(enabled ? "§a水蛭妖对话音乐已开启" : "§7水蛭妖对话音乐已关闭"), false); return 1;
    }
    private static int viewLeechMusicCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        ctx.getSource().sendSuccess(() -> Component.literal("§d水蛭妖对话音乐：" + (RelationshipManager.isDialogueMusicEnabled(p) ? "§a开启" : "§7关闭")), false); return 1;
    }
    private static int grantBloodTechniqueCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0; RelationshipManager.grantBloodTechnique(p, true);
        ctx.getSource().sendSuccess(() -> Component.literal("§d已授予换血功使用权限"), false); return 1;
    }
    private static int getThunderTechniqueCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        ItemStack stack = new ItemStack(ModItems.THUNDER_ACCEL_TECHNIQUE.get());
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        ctx.getSource().sendSuccess(() -> Component.literal("§b已获得雷法功法，右键物品即可切换雷法加速"), false);
        return 1;
    }
    private static int grantDivineBloodPermissionCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0; RelationshipManager.grantDivineBloodPermission(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§d已授予东方神血使用权限"), false); return 1;
    }

    private static int becomeOrientalClanCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        PureFireSkillManager.becomeOrientalClan(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已将玩家转为东方一族，自带纯质阳炎一技能并解锁天赋树权限"), false);
        return 1;
    }

    private static int grantPureFirePermissionCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        PureFireSkillManager.grantPermission(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已授予纯质阳炎控火权限，装配一技能并解锁五技能纯阳护体"), false);
        return 1;
    }

    private static int unlockPureFireSkillCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int skill = IntegerArgumentType.getInteger(ctx, "skill");
        PureFireSkillManager.unlockSkill(p, skill);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已解锁纯质阳炎技能 " + skill), false);
        return 1;
    }

    private static int unlockAllPureFireSkillsCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        PureFireSkillManager.grantPermission(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§6§l【纯质阳炎】已为玩家解锁全部纯质阳炎技能（技能1~10全开）！"), false);
        return 1;
    }

    private static int setPureFireProficiencyCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int value = IntegerArgumentType.getInteger(ctx, "value");
        p.getPersistentData().putInt(PureFireSkillManager.NBT_PROFICIENCY, value);
        PureFireSkillManager.sync(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§6控火熟练度已设置为 " + value), false);
        return 1;
    }

    private static int addPureFireTalentCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PureFireSkillManager.addTalentPoints(p, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已增加 " + amount + " 天赋点，当前天赋点：" + PureFireSkillManager.getTalentPoints(p)), false);
        return 1;
    }

    private static int setPureFireTalentCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PureFireSkillManager.setTalentPoints(p, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已设置天赋点为 " + amount), false);
        return 1;
    }

    private static int viewPureFireTalentCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        int pts = PureFireSkillManager.getTalentPoints(p);
        int layers = PureFireSkillManager.getMeditationLayers(p);
        long cd = PureFireSkillManager.getMeditationCooldownRemaining(p);
        float drop = 10.0F + (layers * 5.0F);
        String cdStr = cd <= 0 ? "已就绪" : String.format("%02d分%02d秒", cd / 60000L, (cd % 60000L) / 1000L);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("§6【纯质阳炎天资】天赋点: %d | 今日冥想: %d/6层 | 击杀掉落概率: %.1f%% | 冥想CD: %s",
                pts, layers, drop, cdStr)), false);
        return 1;
    }

    private static int resetPureFireMeditationCdCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        PureFireSkillManager.resetMeditationCooldown(p);
        ctx.getSource().sendSuccess(() -> Component.literal("§6已重置冥想冷却时间！"), false);
        return 1;
    }

    private static int viewPureFireProficiencyCommand(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = commandPlayer(ctx); if (p == null) return 0;
        ctx.getSource().sendSuccess(() -> Component.literal("§6控火熟练度："
                + PureFireSkillManager.getProficiency(p) + "，已解锁技能掩码："
                + PureFireSkillManager.getUnlockedMask(p)), false);
        return 1;
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer n && event.getOriginal() instanceof ServerPlayer o) {
            VoidTearsSkillManager.copyPlayerData(o, n); RelationshipManager.copyPlayerData(o, n);
            PureFireSkillManager.copyPlayerData(o, n);
        }
    }
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PureFireSkillManager.cancelPureYangFormSelection(p);
            MJNetwork.sendVoidTearsData(p); MJNetwork.sendVoidTearsActive(p, VoidTearsSkillManager.isActive(p)); MJNetwork.sendVoidTearsPoints(p);
            PureFireSkillManager.sync(p);
        }
    }
    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().hasEffect(ModEffects.DIZZINESS.get())) event.setNewTarget(null);
    }
    @SubscribeEvent
    public static void onPlayerChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PureFireSkillManager.cancelPureYangFormSelection(p);
            MJNetwork.sendVoidTearsData(p); MJNetwork.sendVoidTearsActive(p, p.getPersistentData().getBoolean(VoidTearsSkillManager.NBT_VOID_TEARS_ACTIVE)); MJNetwork.sendVoidTearsPoints(p);
            PureFireSkillManager.sync(p);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PureFireSkillManager.cancelPureYangFormSelection(p);
            RelationshipManager.ensureAppearance(p); MJNetwork.sendVoidTearsData(p); MJNetwork.sendVoidTearsActive(p, p.getPersistentData().getBoolean(VoidTearsSkillManager.NBT_VOID_TEARS_ACTIVE)); MJNetwork.sendVoidTearsPoints(p);
            PureFireSkillManager.sync(p);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PureFireSkillManager.cancelPureYangFormSelection(p);
        }
    }
    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.EHUI_SHOU.get(), EHuiShouEntity.createAttributes().build());
        event.put(ModEntities.SHEN_QING_SHUI_ZHI_YAO.get(), com.mjmod.entity.ShenQingShuiZhiYaoEntity.createAttributes().build());
        event.put(ModEntities.ORIENTAL_CLAN.get(), com.mjmod.entity.OrientalClanEntity.createAttributes().build());
    }
    private static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.SHEN_QING_SHUI_ZHI_YAO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Mob.checkMobSpawnRules(type, level, reason, pos, random) && random.nextFloat() < 0.10F, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.ORIENTAL_CLAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Mob.checkMobSpawnRules(type, level, reason, pos, random) && random.nextFloat() < 0.10F, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
