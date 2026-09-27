package com.mjmod.skill;

import com.mjmod.entity.PureFireVortexEntity;
import com.mjmod.network.MJNetwork;
import com.mjmod.registry.ModEntities;
import com.mjmod.registry.ModEffects;

import com.mjmod.registry.ModParticles;
import com.mjmod.entity.PureFirePunchEntity;
import com.mjmod.entity.PureFireMeteorDiveEntity;
import com.mjmod.entity.PureFireDomainEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Pure-fire's new foundation.  This manager deliberately owns only the new
 * pure-fire state, so it cannot alter Void Tears or the other existing skills.
 */
public final class PureFireSkillManager {
    private PureFireSkillManager() {}

    public static final int MAX_SLOTS = 8;
    public static final int SKILL_SWIRL_FLAME = 1;
    public static final int SKILL_HAND_FORM = 2;
    public static final int SKILL_STRAIGHT_PUNCH = 3;
    public static final int SKILL_METEOR_DIVE = 4;
    public static final int SKILL_PURE_YANG_BODY_PROTECTION = 5;
    public static final int SKILL_WIDE_CLEAVE = 6;
    public static final int SKILL_THRUST = 7;
    public static final int SKILL_FIRE_WALL = 8;
    public static final int SKILL_FIRE_BALL = 9;
    public static final int SKILL_DOMAIN = 10;
    public static final int SKILL_COUNT = 10;

    /** A/B/D are the three selectable pure-fire operating schemes. */
    public static final int SCHEME_A = 0;
    public static final int SCHEME_B = 1;
    public static final int SCHEME_D = 2;
    public static final int SCHEME_COUNT = 3;

    /** Percentage modifiers used by the server-authoritative damage path. */
    public static final float SCHEME_B_DAMAGE_REDUCTION = 0.40F;
    public static final float SCHEME_D_DAMAGE_REDUCTION = 0.30F;
    public static final float PURE_YANG_BODY_DAMAGE_REDUCTION = 0.30F;

    public static final String NBT_PERMISSION = "mjmod_pure_fire_permission";
    public static final String NBT_UNLOCKED = "mjmod_pure_fire_unlocked_skills";
    public static final String NBT_SELECTED_SLOT = "mjmod_pure_fire_selected_slot";
    public static final String NBT_SELECTED_SCHEME = "mjmod_pure_fire_selected_scheme";
    public static final String NBT_SCHEME_ACTIVE = "mjmod_pure_fire_scheme_active";
    public static final String NBT_PROFICIENCY = "mjmod_pure_fire_proficiency";
    public static final String NBT_MEDITATING = "mjmod_pure_fire_meditating";
    public static final String NBT_MEDITATION_PROGRESS = "mjmod_pure_fire_meditation_progress";
    public static final String NBT_MEDITATION_DAY = "mjmod_pure_fire_meditation_day";
    public static final String NBT_CASTING = "mjmod_pure_fire_casting";
    public static final String NBT_CAST_START = "mjmod_pure_fire_cast_start";
    public static final String NBT_CAST_STAGE = "mjmod_pure_fire_cast_stage";
    /** True after input is released while the player finishes the cast pose. */
    public static final String NBT_CAST_RELEASED = "mjmod_pure_fire_cast_released";
    /** True only after the cast pose has completed and the visible effect was released. */
    public static final String NBT_CAST_EFFECT_RELEASED = "mjmod_pure_fire_cast_effect_released";
    public static final String NBT_CAST_ACTION_START = "mjmod_pure_fire_cast_action_start";
    public static final String NBT_CAST_LOCK_UNTIL = "mjmod_pure_fire_cast_lock_until";
    public static final String NBT_HAND_FORM = "mjmod_pure_fire_hand_form";
    public static final String NBT_BODY_PROTECTION = "mjmod_pure_yang_body_protection";
    public static final String NBT_BODY_FORM_SELECTING = "mjmod_pure_yang_form_selecting";
    public static final String NBT_BODY_FORM_ORIGINAL = "mjmod_pure_yang_form_original";
    public static final String NBT_DOMAIN_COOLDOWN_UNTIL = "mjmod_pure_fire_domain_cooldown_until";

    public static final String NBT_TALENT_POINTS = "mjmod_pure_fire_talent_points";
    public static final String NBT_MEDITATION_LAYERS = "mjmod_pure_fire_meditation_layers";
    public static final String NBT_MEDITATION_LAST_REAL_TIME = "mjmod_pure_fire_meditation_last_real_time";
    public static final String NBT_MEDITATION_START_TICK = "mjmod_pure_fire_meditation_start_tick";
    public static final String NBT_MEDITATION_LAST_DAY = "mjmod_pure_fire_meditation_last_day";
    public static final String NBT_SLOT_PREFIX = "mjmod_pure_fire_slot_";

    public static final long MEDITATION_COOLDOWN_MILLIS = 24L * 60L * 1000L; // 现实24分钟
    public static final int MEDITATION_DURATION_TICKS = 2 * 60 * 20; // 现实2分钟 = 2400 ticks
    public static final int MAX_MEDITATION_LAYERS = 6;
    public static final int DOMAIN_DURATION_TICKS = 1200;
    public static final int DOMAIN_COOLDOWN_TICKS = 6000;
    /** Query radius covers the farthest horizontal corner of the 150×150 domain. */
    public static final double DOMAIN_QUERY_RADIUS = 110.0D;


    /** Three visible charge phases, one second each (20 game ticks). */
    public static final int CHARGE_TOTAL_TICKS = 60;
    /** The visible effect lasts five seconds after the action finishes. */
    public static final int RELEASE_DURATION_TICKS = 100;
    /** Matches the existing Player Animator palm-release transition. */
    public static final int CAST_ACTION_TICKS = 8;
    private static final int RELEASE_LOCK_TICKS = CAST_ACTION_TICKS + RELEASE_DURATION_TICKS + 8;

    private static final int MEDITATION_REWARD_PER_DAY = 1;
    private static final int MEDITATION_PROGRESS_MAX = 100;

    public static boolean hasPermission(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_PERMISSION);
    }

    public static void grantPermission(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_PERMISSION, true);
        // 解锁全部技能
        for (int skill = SKILL_SWIRL_FLAME; skill <= SKILL_COUNT; skill++) {
            unlockSkill(player, skill);
        }
        awardControlFireGenius(player);
        sync(player);
        player.sendSystemMessage(Component.literal("§6已获得纯质阳炎控火权限（全技能解锁）。"));
    }

    /** 成为东方一族：获得纯质阳炎权限、神血权限，并自带一技能与初始天赋点 */
    public static void becomeOrientalClan(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_PERMISSION, true);
        com.mjmod.skill.RelationshipManager.grantDivineBloodPermission(player);
        unlockSkill(player, SKILL_SWIRL_FLAME); // 自带一技能
        assignSkillToSlot(player, 0, SKILL_SWIRL_FLAME); // 槽位1默认配置一技能
        awardControlFireGenius(player);
        sync(player);
        player.sendSystemMessage(Component.literal("§6§l【东方灵族】你已觉醒东方一族血脉！获得纯阳神火传承，自带纯质阳炎一技能「双手合焰·漩涡火焰」！"));
        player.sendSystemMessage(Component.literal("§e按 O 键即可开启纯质阳炎天赋树进行修炼与技能装配。"));
        player.playSound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
    }

    public static int getUnlockedMask(ServerPlayer player) {
        return player.getPersistentData().getInt(NBT_UNLOCKED);
    }

    public static boolean hasSkill(ServerPlayer player, int skillId) {
        return skillId >= 1 && skillId <= 31
                && (getUnlockedMask(player) & (1 << (skillId - 1))) != 0;
    }

    public static void unlockSkill(ServerPlayer player, int skillId) {
        if (skillId < 1 || skillId > 31) return;
        int mask = getUnlockedMask(player) | (1 << (skillId - 1));
        player.getPersistentData().putInt(NBT_UNLOCKED, mask);
        sync(player);
    }

    public static int getSelectedSlot(ServerPlayer player) {
        int slot = player.getPersistentData().getInt(NBT_SELECTED_SLOT);
        return slot >= 0 && slot < MAX_SLOTS ? slot : 0;
    }

    public static void switchSlot(ServerPlayer player, int slot, boolean reverse) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("§c需要先通过指令获得纯质阳炎控火权限。"));
            return;
        }
        // A slot switch is outside the body-form transaction. This also
        // cleans up a stale selector left by an interrupted client session.
        cancelPureYangFormSelection(player);
        int current = getSelectedSlot(player);
        int next = Math.floorMod(slot, MAX_SLOTS);
        if (slot < 0) next = Math.floorMod(current + (reverse ? -1 : 1), MAX_SLOTS);
        player.getPersistentData().putInt(NBT_SELECTED_SLOT, next);
        int skill = skillInSlot(player, next);
        player.sendSystemMessage(Component.literal(skill == 0
                ? "§7纯质阳炎技能槽 " + (next + 1) + " 为空。"
                : "§6纯质阳炎技能槽 " + (next + 1) + "：" + skillName(skill)));
        sync(player);
    }

    public static int getSelectedScheme(ServerPlayer player) {
        int scheme = player.getPersistentData().getInt(NBT_SELECTED_SCHEME);
        return scheme >= 0 && scheme < SCHEME_COUNT ? scheme : SCHEME_A;
    }

    /** G rotates A → B → D only during the fifth skill's Shift-held selector. */
    public static void cycleScheme(ServerPlayer player, boolean reverse) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("§c需要先通过指令获得纯质阳炎控火权限。"));
            return;
        }
        if (!player.getPersistentData().getBoolean(NBT_BODY_FORM_SELECTING)) {
            player.sendSystemMessage(Component.literal(
                    "§c请先切换到纯阳护体，按住 Shift 再按技能键进入形态选择。"));
            return;
        }
        int current = getSelectedScheme(player);
        int next = Math.floorMod(current + (reverse ? -1 : 1), SCHEME_COUNT);
        player.getPersistentData().putInt(NBT_SELECTED_SCHEME, next);
        player.sendSystemMessage(Component.literal("§e已预选 " + schemeName(next)
                + "：" + schemeDescription(next) + "（松开 Shift 确认）"));
        sync(player);
    }

    public static void beginPureYangFormSelection(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_PURE_YANG_BODY_PROTECTION)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯阳护体。"));
            return;
        }
        if (skillInSlot(player, getSelectedSlot(player)) != SKILL_PURE_YANG_BODY_PROTECTION) {
            player.sendSystemMessage(Component.literal("§c请先把技能切换到纯阳护体。"));
            return;
        }
        cancelPureYangFormSelection(player);
        player.getPersistentData().putBoolean(NBT_BODY_FORM_SELECTING, true);
        player.getPersistentData().putInt(NBT_BODY_FORM_ORIGINAL, getSelectedScheme(player));
        player.sendSystemMessage(Component.literal("§6纯阳护体形态选择：按 G 切换，松开 Shift 使用当前形态。"));
        sync(player);
    }

    public static void cancelPureYangFormSelection(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(NBT_BODY_FORM_SELECTING)) return;
        int original = player.getPersistentData().getInt(NBT_BODY_FORM_ORIGINAL);
        player.getPersistentData().putInt(NBT_SELECTED_SCHEME,
                Math.max(SCHEME_A, Math.min(SCHEME_D, original)));
        player.getPersistentData().putBoolean(NBT_BODY_FORM_SELECTING, false);
        player.getPersistentData().remove(NBT_BODY_FORM_ORIGINAL);
    }

    public static void confirmPureYangFormSelection(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(NBT_BODY_FORM_SELECTING)) return;
        int original = player.getPersistentData().getInt(NBT_BODY_FORM_ORIGINAL);
        int selected = getSelectedScheme(player);
        boolean unchangedActiveForm = isPureYangBodyProtectionActive(player)
                && isSchemeActive(player) && original == selected;
        player.getPersistentData().putBoolean(NBT_BODY_FORM_SELECTING, false);
        player.getPersistentData().remove(NBT_BODY_FORM_ORIGINAL);

        if (unchangedActiveForm) {
            setPureYangBodyProtection(player, false);
            player.getPersistentData().putBoolean(NBT_SCHEME_ACTIVE, false);
            removeSchemeEffects(player);
            player.sendSystemMessage(Component.literal("§7纯阳护体已收回。"));
        } else {
            setPureYangBodyProtection(player, true);
            player.getPersistentData().putBoolean(NBT_SCHEME_ACTIVE, true);
            refreshSchemeEffect(player, true);
            player.sendSystemMessage(Component.literal("§6纯阳护体已切换为 "
                    + schemeName(selected) + "：" + schemeDescription(selected)));
        }
        sync(player);
    }

    public static boolean isSchemeActive(ServerPlayer player) {
        return hasPermission(player)
                && player.getPersistentData().getBoolean(NBT_SCHEME_ACTIVE);
    }

    private static String schemeName(int scheme) {
        return switch (scheme) {
            case SCHEME_B -> "纯质阳炎防御型";
            case SCHEME_D -> "纯质阳炎均衡型";
            default -> "纯质阳炎速度型";
        };
    }

    private static String schemeDescription(int scheme) {
        return switch (scheme) {
            case SCHEME_B -> "紧密护体火焰+强化亮核；攻击+10%，移速-20%，受伤-40%";
            case SCHEME_D -> "均衡双层护体火焰；攻击+30%，移速+30%，受伤-30%";
            default -> "高速外旋护体火焰；攻击+10%，移速+40%";
        };
    }

    private static void removeSchemeEffects(ServerPlayer player) {
        player.removeEffect(ModEffects.PURE_FIRE_SCHEME_A.get());
        player.removeEffect(ModEffects.PURE_FIRE_SCHEME_B.get());
        player.removeEffect(ModEffects.PURE_FIRE_SCHEME_D.get());
    }

    private static net.minecraft.world.effect.MobEffect schemeEffect(int scheme) {
        return switch (scheme) {
            case SCHEME_B -> ModEffects.PURE_FIRE_SCHEME_B.get();
            case SCHEME_D -> ModEffects.PURE_FIRE_SCHEME_D.get();
            default -> ModEffects.PURE_FIRE_SCHEME_A.get();
        };
    }

    /** Keeps exactly one committed scheme effect active and refreshes its short duration. */
    private static void refreshSchemeEffect(ServerPlayer player, boolean force) {
        refreshSchemeEffect(player, getSelectedScheme(player), force);
    }

    private static void refreshSchemeEffect(ServerPlayer player, int scheme, boolean force) {
        net.minecraft.world.effect.MobEffect selected = schemeEffect(scheme);
        net.minecraft.world.effect.MobEffect[] all = {
                ModEffects.PURE_FIRE_SCHEME_A.get(),
                ModEffects.PURE_FIRE_SCHEME_B.get(),
                ModEffects.PURE_FIRE_SCHEME_D.get()
        };
        for (net.minecraft.world.effect.MobEffect effect : all) {
            if (effect != selected) player.removeEffect(effect);
        }
        MobEffectInstance current = player.getEffect(selected);
        if (force || current == null || current.getDuration() <= 6) {
            player.addEffect(new MobEffectInstance(selected, 12, 0,
                    false, true, true));
        }
    }

    public static boolean isPureYangBodyProtectionActive(ServerPlayer player) {
        return hasPermission(player)
                && player.getPersistentData().getBoolean(NBT_BODY_PROTECTION);
    }

    private static void setPureYangBodyProtection(ServerPlayer player, boolean active) {
        player.getPersistentData().putBoolean(NBT_BODY_PROTECTION, active);
        if (active) {
            player.addEffect(new MobEffectInstance(ModEffects.PURE_YANG_BODY_PROTECTION.get(),
                    12, 0, false, true, true));
        } else {
            player.removeEffect(ModEffects.PURE_YANG_BODY_PROTECTION.get());
        }
    }

    /** Applies all pure-fire defensive modifiers as percentages on the server. */
    public static float modifyIncomingDamage(LivingEntity entity, float amount) {
        if (amount <= 0.0F) return amount;
        float multiplier = 1.0F;
        if (entity.hasEffect(ModEffects.PURE_YANG_BODY_PROTECTION.get())) {
            multiplier *= 1.0F - PURE_YANG_BODY_DAMAGE_REDUCTION;
        }
        if (entity.hasEffect(ModEffects.PURE_FIRE_SCHEME_B.get())) {
            multiplier *= 1.0F - SCHEME_B_DAMAGE_REDUCTION;
        }
        if (entity.hasEffect(ModEffects.PURE_FIRE_SCHEME_D.get())) {
            multiplier *= 1.0F - SCHEME_D_DAMAGE_REDUCTION;
        }
        return amount * multiplier;
    }

    /** 纯质阳炎技能伤害在阳炎领域内获得 1.5 倍乘区增伤。 */
    public static float modifyPureFireDamage(LivingEntity owner, float amount) {
        if (!(owner instanceof ServerPlayer player) || amount <= 0.0F) return amount;
        boolean active = player.serverLevel().getEntitiesOfClass(
                PureFireDomainEntity.class, player.getBoundingBox().inflate(DOMAIN_QUERY_RADIUS),
                domain -> domain.getOwner() == player && domain.contains(player) && !domain.isRemoved())
                .stream().findAny().isPresent();
        return active ? amount * 1.5F : amount;
    }

    public static int skillInSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) return 0;
        String key = NBT_SLOT_PREFIX + slot;
        if (player.getPersistentData().contains(key)) {
            int skill = player.getPersistentData().getInt(key);
            return (skill != 0 && hasSkill(player, skill)) ? skill : 0;
        }
        // 默认初始装配：槽位 i 对应技能 i+1
        int defaultSkill = slot + 1;
        return hasSkill(player, defaultSkill) ? defaultSkill : 0;
    }

    public static int[] getEquippedSkills(ServerPlayer player) {
        int[] skills = new int[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            skills[i] = skillInSlot(player, i);
        }
        return skills;
    }

    public static void assignSkillToSlot(ServerPlayer player, int slot, int skillId) {
        if (!hasPermission(player)) return;
        if (slot < 0 || slot >= MAX_SLOTS) return;
        if (skillId != 0 && !hasSkill(player, skillId)) {
            player.sendSystemMessage(Component.literal("§c尚未掌握该纯质阳炎技能！"));
            return;
        }
        player.getPersistentData().putInt(NBT_SLOT_PREFIX + slot, skillId);
        player.sendSystemMessage(Component.literal("§6已将技能槽 " + (slot + 1) + " 配置为：" + (skillId == 0 ? "空" : skillName(skillId))));
        sync(player);
    }

    public static int getTalentPoints(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(NBT_TALENT_POINTS));
    }

    public static void setTalentPoints(ServerPlayer player, int points) {
        player.getPersistentData().putInt(NBT_TALENT_POINTS, Math.max(0, points));
        sync(player);
    }

    public static void addTalentPoints(ServerPlayer player, int points) {
        if (points == 0) return;
        setTalentPoints(player, getTalentPoints(player) + points);
    }

    public static int getMeditationLayers(ServerPlayer player) {
        checkMeditationDayReset(player);
        return Math.max(0, Math.min(MAX_MEDITATION_LAYERS, player.getPersistentData().getInt(NBT_MEDITATION_LAYERS)));
    }

    public static void setMeditationLayers(ServerPlayer player, int layers) {
        player.getPersistentData().putInt(NBT_MEDITATION_LAYERS, Math.max(0, Math.min(MAX_MEDITATION_LAYERS, layers)));
        sync(player);
    }

    public static long getMeditationCooldownRemaining(ServerPlayer player) {
        long last = player.getPersistentData().getLong(NBT_MEDITATION_LAST_REAL_TIME);
        long now = System.currentTimeMillis();
        long diff = now - last;
        return diff < MEDITATION_COOLDOWN_MILLIS ? (MEDITATION_COOLDOWN_MILLIS - diff) : 0L;
    }

    public static void resetMeditationCooldown(ServerPlayer player) {
        player.getPersistentData().remove(NBT_MEDITATION_LAST_REAL_TIME);
        sync(player);
    }

    private static void checkMeditationDayReset(ServerPlayer player) {
        long currentDay = player.serverLevel().getDayTime() / 24000L;
        long lastDay = player.getPersistentData().getLong(NBT_MEDITATION_LAST_DAY);
        if (currentDay != lastDay) {
            player.getPersistentData().putLong(NBT_MEDITATION_LAST_DAY, currentDay);
            player.getPersistentData().putInt(NBT_MEDITATION_LAYERS, 0);
        }
    }

    public static void buyTalentSkill(ServerPlayer player, int skillId) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("§c尚未领悟纯质阳炎，无法点亮天赋。"));
            return;
        }
        if (skillId < 1 || skillId > SKILL_COUNT) return;
        if (hasSkill(player, skillId)) {
            player.sendSystemMessage(Component.literal("§e该技能已领悟。"));
            return;
        }
        // 天赋技能前置：一技能之后解锁其他，或技能前置依赖
        if (skillId > 1 && !hasSkill(player, 1)) {
            player.sendSystemMessage(Component.literal("§c必须先领悟纯质阳炎基础（一技能）。"));
            return;
        }
        if (skillId == SKILL_METEOR_DIVE && !hasSkill(player, SKILL_HAND_FORM)) {
            player.sendSystemMessage(Component.literal("§c四技能【增压】必须先领悟并开启二技能双手纯质阳炎。"));
            return;
        }
        int cost = 4;
        int currentPoints = getTalentPoints(player);
        if (currentPoints < cost) {
            player.sendSystemMessage(Component.literal("§c天赋点不足！领悟该技能需要 " + cost + " 点天赋点（当前拥有: " + currentPoints + "）"));
            return;
        }
        setTalentPoints(player, currentPoints - cost);
        unlockSkill(player, skillId);
        addProficiency(player, 5);
        player.sendSystemMessage(Component.literal("§6§l领悟成功！成功习得「" + skillName(skillId) + "」！"));
        player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0F, 1.2F);
        sync(player);
    }

    private static String skillName(int skill) {
        return switch (skill) {
            case SKILL_SWIRL_FLAME -> "双手合焰·漩涡火焰";
            case SKILL_HAND_FORM -> "纯质阳炎·双手附焰";
            case SKILL_STRAIGHT_PUNCH -> "纯质阳炎·直冲拳";
            case SKILL_METEOR_DIVE -> "纯质阳炎·增压";
            case SKILL_PURE_YANG_BODY_PROTECTION -> "纯阳护体";
            case SKILL_WIDE_CLEAVE -> "纯质阳炎·横斩";
            case SKILL_THRUST -> "纯质阳炎·穿刺";
            case SKILL_FIRE_WALL -> "纯质阳炎·火墙";
            case SKILL_FIRE_BALL -> "纯质阳炎·烈阳火球";
            case SKILL_DOMAIN -> "纯质阳炎·阳炎领域";
            default -> "未知技能";
        };
    }

    public static int getProficiency(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(NBT_PROFICIENCY));
    }

    public static void addProficiency(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        // The Control Fire Genius achievement is the permanent double-XP
        // modifier. Granting permission unlocks it for the initial build,
        // while future skill discoveries also call this path.
        int multiplier = hasControlFireGenius(player) ? 2 : 1;
        player.getPersistentData().putInt(NBT_PROFICIENCY,
                getProficiency(player) + amount * multiplier);
        sync(player);
    }

    public static boolean hasControlFireGenius(ServerPlayer player) {
        return player.getPersistentData().getBoolean("mjmod_control_fire_genius");
    }

    private static void awardControlFireGenius(ServerPlayer player) {
        if (!hasControlFireGenius(player)) {
            player.getPersistentData().putBoolean("mjmod_control_fire_genius", true);
            Advancement advancement = player.server.getAdvancements().getAdvancement(
                    new ResourceLocation("mjmod", "pure_fire/control_fire_genius"));
            if (advancement != null) player.getAdvancements().award(advancement, "unlocked");
        }
    }

    public static boolean isMeditating(ServerPlayer player) {
        return hasPermission(player)
                && player.getPersistentData().getBoolean(NBT_MEDITATING);
    }

    public static void toggleMeditation(ServerPlayer player) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("§c需要先通过指令获得纯质阳炎控火权限。"));
            return;
        }
        boolean wasMeditating = isMeditating(player);
        if (wasMeditating) {
            // 中断或退出冥想
            player.getPersistentData().putBoolean(NBT_MEDITATING, false);
            player.getPersistentData().remove(NBT_MEDITATION_START_TICK);
            player.sendSystemMessage(Component.literal("§7已退出凝神冥想。"));
            sync(player);
            return;
        }

        // 检查每日层数上限
        int layers = getMeditationLayers(player);
        if (layers >= MAX_MEDITATION_LAYERS) {
            player.sendSystemMessage(Component.literal("§c今日冥想境界已圆满（已叠加满 " + MAX_MEDITATION_LAYERS + " 层），明日再来吧。"));
            return;
        }

        // 检查现实24分钟冷却
        long cd = getMeditationCooldownRemaining(player);
        if (cd > 0) {
            long sec = cd / 1000L;
            long min = sec / 60L;
            long remSec = sec % 60L;
            player.sendSystemMessage(Component.literal(String.format("§c心火浮躁，还需等待 %02d分%02d秒 方可再次入定冥想。", min, remSec)));
            return;
        }

        // 开始冥想
        player.getPersistentData().putBoolean(NBT_MEDITATING, true);
        player.getPersistentData().putLong(NBT_MEDITATION_START_TICK, player.serverLevel().getGameTime());
        player.sendSystemMessage(Component.literal("§6§l已进入凝神冥想... 需持续入定2分钟以吸收天地火气。"));
        sync(player);
    }

    public static void meditationPulse(ServerPlayer player) {
        if (!isMeditating(player)) return;
        int progress = player.getPersistentData().getInt(NBT_MEDITATION_PROGRESS) + 5;
        if (progress >= MEDITATION_PROGRESS_MAX) {
            progress = 0;
            addProficiency(player, 1);
            player.sendSystemMessage(Component.literal("§6控火熟练度提升！当前：" + getProficiency(player)));
        }
        player.getPersistentData().putInt(NBT_MEDITATION_PROGRESS, progress);
        sync(player);
    }

    public static int getMeditationProgress(ServerPlayer player) {
        return Math.max(0, Math.min(MEDITATION_PROGRESS_MAX,
                player.getPersistentData().getInt(NBT_MEDITATION_PROGRESS)));
    }

    public static boolean startCasting(ServerPlayer player) {
        if (!hasPermission(player) || skillInSlot(player, getSelectedSlot(player)) != SKILL_SWIRL_FLAME) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎一技能，请使用控火权限/技能指令。"));
            return false;
        }
        if (!canStartSwirlFlame(player)) {
            player.sendSystemMessage(Component.literal("§c释放一技能前，主手必须为空，或已开启双手纯质阳炎。"));
            return false;
        }
        if (isMeditating(player)) toggleMeditation(player);
        if (isCasting(player)) return false;
        player.getPersistentData().putBoolean(NBT_CASTING, true);
        player.getPersistentData().putBoolean(NBT_CAST_RELEASED, false);
        player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
        player.getPersistentData().putInt(NBT_CAST_STAGE, 0);
        player.getPersistentData().remove(NBT_CAST_LOCK_UNTIL);
        player.getPersistentData().remove(NBT_CAST_ACTION_START);
        player.getPersistentData().putLong(NBT_CAST_START, player.serverLevel().getGameTime());
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 origin = getCastOrigin(player, view);
        PureFireVortexEntity entity = new PureFireVortexEntity(
                player.serverLevel(), player, origin, view);
        player.serverLevel().addFreshEntity(entity);
        sync(player);
        return true;
    }

    public static void stopCasting(ServerPlayer player) {
        if (!isCasting(player)) return;
        if (!canStartSwirlFlame(player)) {
            player.getPersistentData().putBoolean(NBT_CASTING, false);
            player.getPersistentData().putBoolean(NBT_CAST_RELEASED, false);
            player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
            player.getPersistentData().putInt(NBT_CAST_STAGE, 0);
            player.getPersistentData().remove(NBT_CAST_START);
            player.getPersistentData().remove(NBT_CAST_ACTION_START);
            findFlames(player).forEach(PureFireVortexEntity::cancelCharge);
            player.sendSystemMessage(Component.literal("§c释放一技能失败：松开时主手必须为空，或已开启双手纯质阳炎。"));
            sync(player);
            return;
        }
        float progress = getChargeProgress(player);
        // A tap before the first completed phase is treated as an accidental
        // preview.  Releasing in phase 1, 2 or 3 first completes the palm
        // action; only then does the visible effect appear.
        int chargeStage = getChargeStage(player);
        if (chargeStage <= 0 || progress < (1.0F / 3.0F)) {
            player.getPersistentData().putBoolean(NBT_CASTING, false);
            player.getPersistentData().putBoolean(NBT_CAST_RELEASED, false);
            player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
            player.getPersistentData().putInt(NBT_CAST_STAGE, 0);
            player.getPersistentData().remove(NBT_CAST_START);
            player.getPersistentData().remove(NBT_CAST_ACTION_START);
            findFlames(player).forEach(PureFireVortexEntity::cancelCharge);
            player.sendSystemMessage(Component.literal("§7纯质阳炎蓄力未完成，技能已取消。"));
        } else {
            player.getPersistentData().putBoolean(NBT_CAST_RELEASED, true);
            player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
            player.getPersistentData().putInt(NBT_CAST_STAGE, chargeStage);
            player.getPersistentData().putLong(NBT_CAST_ACTION_START,
                    player.serverLevel().getGameTime());
            player.getPersistentData().putLong(NBT_CAST_LOCK_UNTIL,
                    player.serverLevel().getGameTime() + RELEASE_LOCK_TICKS);
            // The entity remains hidden until tick CAST_ACTION_TICKS.  This
            // deliberately separates the hand action from the attack effect.
        }
        sync(player);
    }

    /** Called when a player disconnects or otherwise loses the input release
     * packet, preventing a charged entity from remaining attached forever. */
    public static void cancelCasting(ServerPlayer player) {
        if (!isCasting(player)) return;
        player.getPersistentData().putBoolean(NBT_CASTING, false);
        player.getPersistentData().putBoolean(NBT_CAST_RELEASED, false);
        player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
        player.getPersistentData().putInt(NBT_CAST_STAGE, 0);
        player.getPersistentData().remove(NBT_CAST_START);
        player.getPersistentData().remove(NBT_CAST_ACTION_START);
        findFlames(player).forEach(PureFireVortexEntity::cancelCharge);
        sync(player);
    }

    public static boolean isCasting(ServerPlayer player) {
        return hasPermission(player)
                && player.getPersistentData().getBoolean(NBT_CASTING);
    }

    public static boolean isHandFormActive(ServerPlayer player) {
        return hasPermission(player) && player.getPersistentData().getBoolean(NBT_HAND_FORM);
    }

    public static void activateSelectedSkill(ServerPlayer player) {
        activateSelectedSkill(player, false);
    }

    /**
     * Activates an ordinary five-slot skill. Pure Yang Body Protection uses
     * begin/cycle/confirm so releasing Shift is the transaction boundary.
     */
    public static void activateSelectedSkill(ServerPlayer player, boolean withScheme) {
        int skill = skillInSlot(player, getSelectedSlot(player));
        boolean activated;
        if (skill == SKILL_SWIRL_FLAME) {
            activated = startCasting(player);
        } else if (skill == SKILL_HAND_FORM) {
            activated = toggleHandForm(player);
        } else if (skill == SKILL_STRAIGHT_PUNCH) {
            activated = performStraightPunch(player);
        } else if (skill == SKILL_METEOR_DIVE) {
            activated = performMeteorDive(player);
        } else if (skill == SKILL_PURE_YANG_BODY_PROTECTION) {
            player.sendSystemMessage(Component.literal(
                    "§c请按住 Shift 再按技能键，按 G 选择形态，松开 Shift 使用。"));
            activated = false;
        } else if (skill == SKILL_WIDE_CLEAVE) {
            activated = performWideCleave(player);
        } else if (skill == SKILL_THRUST) {
            activated = performThrust(player);
        } else if (skill == SKILL_FIRE_WALL) {
            activated = performFireWall(player);
        } else if (skill == SKILL_FIRE_BALL) {
            activated = performFireBall(player);
        } else if (skill == SKILL_DOMAIN) {
            activated = performDomain(player);
        } else {
            player.sendSystemMessage(Component.literal("§c当前纯质阳炎技能槽为空或尚未解锁。"));
            activated = false;
        }

    }

    private static boolean toggleHandForm(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_HAND_FORM)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎二技能。"));
            return false;
        }
        boolean active = !isHandFormActive(player);
        if (active && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) {
            player.sendSystemMessage(Component.literal("§c开启双手纯质阳炎前，主手和副手都必须为空。"));
            return false;
        }
        player.getPersistentData().putBoolean(NBT_HAND_FORM, active);
        if (active) {
            player.addEffect(new MobEffectInstance(ModEffects.PURE_FIRE_HAND_FORM.get(),
                    10, 0, false, false, false));
        } else {
            player.removeEffect(ModEffects.PURE_FIRE_HAND_FORM.get());
        }
        player.sendSystemMessage(Component.literal(active
                ? "§6纯质阳炎已附着双手，攻击力提升50%。"
                : "§7双手纯质阳炎已收回。"));
        sync(player);
        return true;
    }

    private static boolean performStraightPunch(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_STRAIGHT_PUNCH)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎三技能。"));
            return false;
        }
        if (!isHandFormActive(player)) {
            player.sendSystemMessage(Component.literal("§c必须先开启二技能双手纯质阳炎，才能使用三技能。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c释放纯质阳炎前，主手必须为空。请先收回双手纯质阳炎或卸下主手物品。"));
            return false;
        }
        if (player.serverLevel().getEntitiesOfClass(PureFirePunchEntity.class,
                player.getBoundingBox().inflate(3.0D), entity -> entity.getOwner() == player)
                .stream().findAny().isPresent()) return false;
        player.serverLevel().addFreshEntity(new PureFirePunchEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    private static boolean performMeteorDive(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_METEOR_DIVE)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎四技能。"));
            return false;
        }
        if (!isHandFormActive(player)) {
            player.sendSystemMessage(Component.literal("§c必须先开启二技能双手纯质阳炎，才能使用四技能【增压】。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c释放纯质阳炎前，主手必须为空。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                PureFireMeteorDiveEntity.class, player.getBoundingBox().inflate(48.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;
        player.serverLevel().addFreshEntity(
                new PureFireMeteorDiveEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    /** 七、八技能结束后，火刃留在右手上五秒。 */
    public static final int BLADE_HOLD_TICKS = 100;

    public static void holdFireBlade(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(ModEffects.PURE_FIRE_BLADE_HOLD.get(),
                BLADE_HOLD_TICKS, 0, false, true, true));
    }

    /** 火刃留手期间的近战命中附加重伤，持续五秒。 */
    public static void applyBladeHoldWound(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null || target == attacker || !target.isAlive()) return;
        if (!attacker.hasEffect(ModEffects.PURE_FIRE_BLADE_HOLD.get())) return;
        target.addEffect(new MobEffectInstance(ModEffects.SEVERELY_WOUNDED.get(),
                BLADE_HOLD_TICKS, 0, false, true, true));
        target.setSecondsOnFire(3);
    }

    private static boolean performWideCleave(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_WIDE_CLEAVE)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎六技能【横斩】。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c施展横斩前，主手必须为空。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                com.mjmod.entity.PureFireCleaveEntity.class, player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;
        player.serverLevel().addFreshEntity(
                new com.mjmod.entity.PureFireCleaveEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    private static boolean performThrust(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_THRUST)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎七技能【穿刺】。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c施展穿刺前，主手必须为空。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                com.mjmod.entity.PureFireThrustEntity.class, player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;
        player.serverLevel().addFreshEntity(
                new com.mjmod.entity.PureFireThrustEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    private static boolean performFireWall(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_FIRE_WALL)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎八技能【火墙】。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c施展火墙前，主手必须为空。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                com.mjmod.entity.PureFireWallCastEntity.class, player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;
        player.serverLevel().addFreshEntity(
                new com.mjmod.entity.PureFireWallCastEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    private static boolean performFireBall(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_FIRE_BALL)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎【烈阳火球】技能。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c施展烈阳火球前，主手必须为空。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                com.mjmod.entity.PureFireBallCastEntity.class, player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;
        player.serverLevel().addFreshEntity(
                new com.mjmod.entity.PureFireBallCastEntity(player.serverLevel(), player));
        sync(player);
        return true;
    }

    /** 十技能：固定中心展开 150×150×60 阳炎领域，开启即进入 5 分钟冷却。 */
    private static boolean performDomain(ServerPlayer player) {
        if (!hasPermission(player) || !hasSkill(player, SKILL_DOMAIN)) {
            player.sendSystemMessage(Component.literal("§c尚未解锁纯质阳炎十技能【阳炎领域】。"));
            return false;
        }
        if (!canReleaseWithMainHand(player)) {
            player.sendSystemMessage(Component.literal("§c展开阳炎领域前，主手必须为空。"));
            return false;
        }
        long now = player.serverLevel().getGameTime();
        long cooldownUntil = player.getPersistentData().getLong(NBT_DOMAIN_COOLDOWN_UNTIL);
        if (cooldownUntil > now) {
            long remaining = cooldownUntil - now;
            player.sendSystemMessage(Component.literal("§c阳炎领域尚在冷却，还需 "
                    + ((remaining + 19L) / 20L) + " 秒。"));
            return false;
        }
        boolean active = player.serverLevel().getEntitiesOfClass(
                PureFireDomainEntity.class, player.getBoundingBox().inflate(DOMAIN_QUERY_RADIUS),
                entity -> entity.getOwner() == player && !entity.isRemoved())
                .stream().findAny().isPresent();
        if (active) return false;

        if (!player.serverLevel().addFreshEntity(new PureFireDomainEntity(player.serverLevel(), player))) {
            player.sendSystemMessage(Component.literal("§c阳炎领域展开失败，请稍后重试。"));
            return false;
        }
        player.getPersistentData().putLong(NBT_DOMAIN_COOLDOWN_UNTIL,
                now + DOMAIN_COOLDOWN_TICKS);
        player.sendSystemMessage(Component.literal("§6§l【阳炎领域】领域展开！持续60秒，冷却已开始。"));
        sync(player);
        return true;
    }

    public static void stopFireBallCasting(ServerPlayer player) {
        List<com.mjmod.entity.PureFireBallCastEntity> casts = player.serverLevel().getEntitiesOfClass(
                com.mjmod.entity.PureFireBallCastEntity.class, player.getBoundingBox().inflate(16.0D),
                entity -> entity.getOwner() == player && !entity.isRemoved());
        for (com.mjmod.entity.PureFireBallCastEntity cast : casts) {
            cast.releaseCast(player.getViewVector(1.0F));
        }
    }

    public static boolean isReleasing(ServerPlayer player) {
        return isCasting(player)
                && player.getPersistentData().getBoolean(NBT_CAST_RELEASED);
    }

    public static float getChargeProgress(ServerPlayer player) {
        if (!isCasting(player)) return 0.0F;
        long elapsed = Math.max(0L, player.serverLevel().getGameTime()
                - player.getPersistentData().getLong(NBT_CAST_START));
        return Math.min(1.0F, elapsed / (float) CHARGE_TOTAL_TICKS);
    }

    /** 1, 2 and 3 correspond to weak, medium and strong charge. */
    public static int getChargeStage(ServerPlayer player) {
        if (!isCasting(player)) return 0;
        long elapsed = Math.max(0L, player.serverLevel().getGameTime()
                - player.getPersistentData().getLong(NBT_CAST_START));
        // Before 20 ticks no stage is complete.  Then the three exact stages
        // are 20-39, 40-59 and 60+ ticks.
        if (elapsed < 20L) return 0;
        return Math.min(3, (int) ((elapsed - 20L) / 20L) + 1);
    }

    /** Exact attack reach for each completed charge stage. */
    public static int getRangeForStage(int stage) {
        return switch (Math.max(1, Math.min(3, stage))) {
            case 1 -> 5;
            case 2 -> 10;
            default -> 15;
        };
    }

    private static List<PureFireVortexEntity> findFlames(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(
                PureFireVortexEntity.class, player.getBoundingBox().inflate(32.0D),
                flame -> flame.getOwner() == player);
    }

    public static void tick(ServerPlayer player) {
        if (!hasPermission(player)) return;
        if (isSchemeActive(player)) {
            // During selection NBT_SELECTED_SCHEME is only a preview. Keep the
            // previously committed form authoritative until Shift is released.
            int committedScheme = player.getPersistentData().getBoolean(NBT_BODY_FORM_SELECTING)
                    ? player.getPersistentData().getInt(NBT_BODY_FORM_ORIGINAL)
                    : getSelectedScheme(player);
            refreshSchemeEffect(player, committedScheme, false);
        }
        if (isPureYangBodyProtectionActive(player)) {
            player.addEffect(new MobEffectInstance(ModEffects.PURE_YANG_BODY_PROTECTION.get(),
                    12, 0, false, true, true));
        }
        if (isMeditating(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 255,
                    false, false, false));
            long startTick = player.getPersistentData().getLong(NBT_MEDITATION_START_TICK);
            long elapsed = player.serverLevel().getGameTime() - startTick;
            int progress = (int) Math.min(100, (elapsed * 100) / MEDITATION_DURATION_TICKS);
            player.getPersistentData().putInt(NBT_MEDITATION_PROGRESS, progress);
            
            // 环绕纯阳粒子
            if (player.tickCount % 4 == 0) {
                double angle = (player.tickCount * 0.15D) % (Math.PI * 2);
                double rx = Math.cos(angle) * 0.85D;
                double rz = Math.sin(angle) * 0.85D;
                player.serverLevel().sendParticles(ModParticles.PURE_FIRE.get(),
                        player.getX() + rx, player.getY() + 0.3D, player.getZ() + rz,
                        2, 0.05, 0.05, 0.05, 0.01);
            }

            // 完成2分钟入定
            if (elapsed >= MEDITATION_DURATION_TICKS) {
                player.getPersistentData().putBoolean(NBT_MEDITATING, false);
                player.getPersistentData().remove(NBT_MEDITATION_START_TICK);
                player.getPersistentData().putInt(NBT_MEDITATION_PROGRESS, 0);
                player.getPersistentData().putLong(NBT_MEDITATION_LAST_REAL_TIME, System.currentTimeMillis());
                int currentLayers = getMeditationLayers(player) + 1;
                setMeditationLayers(player, currentLayers);
                addProficiency(player, 5);
                player.sendSystemMessage(Component.literal("§6§l【纯质阳炎】心火归元！冥想圆满完成！"));
                player.sendSystemMessage(Component.literal("§e当前冥想加成：§6" + currentLayers + " / " + MAX_MEDITATION_LAYERS + " 层§7（敌怪击杀天赋点掉落率额外 +" + (currentLayers * 5) + "%）"));
                player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                sync(player);
            } else if (player.tickCount % 20 == 0) {
                sync(player);
            }
        }
        if (isHandFormActive(player)) {
            // The hand effect and its particles are rendered on the client
            // after each arm bone transform; no drifting world-space estimate.
            player.addEffect(new MobEffectInstance(ModEffects.PURE_FIRE_HAND_FORM.get(),
                    10, 0, false, false, false));
            if (player.tickCount % 2 == 0) {
                spawnHandFormParticles(player);
            }
        }
        if (isCasting(player)) {
            int stage = getChargeStage(player);
            if (!isReleasing(player)) {
                applyChargingSlowdown(player, stage);
                int previousStage = player.getPersistentData().getInt(NBT_CAST_STAGE);
                if (stage > previousStage) {
                    player.getPersistentData().putInt(NBT_CAST_STAGE, stage);
                    spawnChargeStageEffect(player, stage);
                    sync(player);
                }
                if (player.tickCount % 6 == 0) {
                    spawnChargingHandFlames(player);
                }
            }
            // Keep a single live model per player if a duplicate packet arrives.
            List<PureFireVortexEntity> flames = findFlames(player);
            flames.stream().skip(1).forEach(Entity::discard);
        }
        if (isReleasing(player)) {
            int stage = player.getPersistentData().getInt(NBT_CAST_STAGE);
            applyChargingSlowdown(player, stage);
            long actionStart = player.getPersistentData().getLong(NBT_CAST_ACTION_START);
            if (!player.getPersistentData().getBoolean(NBT_CAST_EFFECT_RELEASED)
                    && player.serverLevel().getGameTime() >= actionStart + CAST_ACTION_TICKS) {
                // The player has completed the action. Release the exact
                // stage-sized effect only now, never on mouse-up itself.
                findFlames(player).forEach(flame -> flame.release(stage));
                player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, true);
                sync(player);
            }
            if (player.serverLevel().getGameTime()
                    >= player.getPersistentData().getLong(NBT_CAST_LOCK_UNTIL)) {
                player.getPersistentData().putBoolean(NBT_CASTING, false);
                player.getPersistentData().putBoolean(NBT_CAST_RELEASED, false);
                player.getPersistentData().putBoolean(NBT_CAST_EFFECT_RELEASED, false);
                player.getPersistentData().putInt(NBT_CAST_STAGE, 0);
                player.getPersistentData().remove(NBT_CAST_START);
                player.getPersistentData().remove(NBT_CAST_ACTION_START);
                player.getPersistentData().remove(NBT_CAST_LOCK_UNTIL);
                sync(player);
            }
        }
    }

    /** A sparse pure-fire particle cue around the two skill-2 fists. */
    private static void spawnHandFormParticles(ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 base = player.position().add(0.0D, 0.78D, 0.0D)
                .add(forward.scale(0.18D));
        ServerLevel level = player.serverLevel();
        Vec3 leftHand = base.add(right.scale(-0.36D));
        Vec3 rightHand = base.add(right.scale(0.36D));
        level.sendParticles(ModParticles.PURE_FIRE.get(), leftHand.x, leftHand.y, leftHand.z,
                1, 0.06D, 0.10D, 0.06D, 0.012D);
        level.sendParticles(ModParticles.PURE_FIRE.get(), rightHand.x, rightHand.y, rightHand.z,
                1, 0.06D, 0.10D, 0.06D, 0.012D);
    }

    /** A sparse custom-particle cue around the two lowered hands while charging. */
    private static void spawnChargingHandFlames(ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 base = player.position().add(0.0D, 0.72D, 0.0D)
                .add(forward.scale(0.10D));
        ServerLevel level = player.serverLevel();
        Vec3 leftHand = base.add(right.scale(-0.34D));
        Vec3 rightHand = base.add(right.scale(0.34D));
        level.sendParticles(ModParticles.PURE_FIRE.get(), leftHand.x, leftHand.y, leftHand.z,
                2, 0.025D, 0.05D, 0.025D, 0.004D);
        level.sendParticles(ModParticles.PURE_FIRE.get(), rightHand.x, rightHand.y, rightHand.z,
                2, 0.025D, 0.05D, 0.025D, 0.004D);
    }


    /** Applies a real, refreshable Slowness effect instead of freezing input. */
    private static void applyChargingSlowdown(ServerPlayer player, int stage) {
        int amplifier = stage <= 1 ? 0 : Math.min(2, stage - 1);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6,
                amplifier, false, false, false));
    }

    /** Pure-fire skills cannot start with any item in the main hand. */
    private static boolean canReleaseWithMainHand(ServerPlayer player) {
        return player.getMainHandItem().isEmpty();
    }

    /** Skill 1 may be fired through the bound hands while skill 2 is active. */
    private static boolean canStartSwirlFlame(ServerPlayer player) {
        return canReleaseWithMainHand(player)
                || player.hasEffect(ModEffects.PURE_FIRE_HAND_FORM.get());
    }



    /**
     * One burst per completed stage: a ring around the caster, a forward
     * range guide, and an endpoint halo.  The guide distances are exactly
     * 5/10/15 blocks so the charge preview and released effect agree.
     */
    private static void spawnChargeStageEffect(ServerPlayer player, int stage) {
        ServerLevel level = player.serverLevel();
        Vec3 forward = getCastingDirection(player);
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        double ringRadius = 0.75D + stage * 0.12D;
        int ringCount = 16 + stage * 4;
        for (int i = 0; i < ringCount; i++) {
            double angle = Math.PI * 2.0D * i / ringCount;
            Vec3 point = center.add(right.scale(Math.cos(angle) * ringRadius))
                    .add(forward.scale(Math.sin(angle) * ringRadius))
                    .add(0.0D, Math.sin(angle * 2.0D) * 0.18D, 0.0D);
            level.sendParticles(ModParticles.PURE_FIRE.get(), point.x, point.y, point.z,
                    1, 0.02D, 0.06D, 0.02D, 0.01D);
        }
        int range = getRangeForStage(stage);
        Vec3 origin = getCastOrigin(player, forward);
        for (double distance = 0.5D; distance <= range; distance += 0.5D) {
            Vec3 point = origin.add(forward.scale(distance));
            level.sendParticles(ModParticles.PURE_FIRE.get(), point.x, point.y, point.z,
                    1, 0.025D, 0.04D, 0.025D, 0.005D);
        }
        Vec3 endpoint = origin.add(forward.scale(range));
        for (int i = 0; i < 8 + stage * 4; i++) {
            double angle = Math.PI * 2.0D * i / (8 + stage * 4);
            level.sendParticles(ModParticles.PURE_FIRE.get(),
                    endpoint.x + Math.cos(angle) * 0.45D,
                    endpoint.y + Math.sin(angle) * 0.45D,
                    endpoint.z + Math.sin(angle) * 0.45D,
                    1, 0.01D, 0.01D, 0.01D, 0.005D);
        }
    }

    /** Uses the entity's latest server-synchronized crosshair direction. */
    private static Vec3 getCastingDirection(ServerPlayer player) {
        return findFlames(player).stream().findFirst()
                .map(PureFireVortexEntity::getFireDirection)
                .orElseGet(() -> player.getViewVector(1.0F).normalize());
    }

    /** Shared right-palm anchor used by the preview and the released entity. */
    private static Vec3 getCastOrigin(ServerPlayer player, Vec3 direction) {
        Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(-Math.cos(Math.toRadians(player.getYRot())),
                    0.0D, -Math.sin(Math.toRadians(player.getYRot())));
        } else {
            right = right.normalize();
        }
        return player.position().add(0.0D, player.getBbHeight() * 0.82D, 0.0D)
                .add(right.scale(0.31D)).add(direction.scale(0.68D));
    }

    /** 击杀敌对生物获得天赋点：基础10% + 冥想每层5% (最多6层=+30%) */
    public static void tryLearnFromKill(ServerPlayer player, LivingEntity killed) {
        if (!hasPermission(player) || !(killed instanceof Mob) || !(killed instanceof Enemy)
                || !killed.isDeadOrDying()) return;
        
        int layers = getMeditationLayers(player);
        float dropChance = 0.10F + (layers * 0.05F);
        if (player.getRandom().nextFloat() < dropChance) {
            addTalentPoints(player, 1);
            int currentPoints = getTalentPoints(player);
            player.sendSystemMessage(Component.literal("§6§l【纯质阳炎】心火有所领悟，获得 1 点天赋点！§7(当前剩余: §e" + currentPoints + "§7)"));
            player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
        }
    }

    private static int firstLockedSkill(ServerPlayer player) {
        for (int i = 2; i <= MAX_SLOTS; i++) if (!hasSkill(player, i)) return i;
        return 0;
    }

    public static void sync(ServerPlayer player) {
        MJNetwork.sendPureFireStatus(player);
    }

    public static void copyPlayerData(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        var old = oldPlayer.getPersistentData();
        var data = newPlayer.getPersistentData();
        String[] keys = {NBT_PERMISSION, NBT_UNLOCKED, NBT_SELECTED_SLOT, NBT_SELECTED_SCHEME,
                NBT_PROFICIENCY, NBT_MEDITATION_PROGRESS, NBT_MEDITATION_DAY,
                NBT_TALENT_POINTS, NBT_MEDITATION_LAYERS, NBT_MEDITATION_LAST_REAL_TIME,
                NBT_MEDITATION_LAST_DAY, NBT_DOMAIN_COOLDOWN_UNTIL,
                NBT_SLOT_PREFIX + "0", NBT_SLOT_PREFIX + "1", NBT_SLOT_PREFIX + "2", NBT_SLOT_PREFIX + "3",
                NBT_SLOT_PREFIX + "4", NBT_SLOT_PREFIX + "5", NBT_SLOT_PREFIX + "6", NBT_SLOT_PREFIX + "7"};
        for (String key : keys) if (old.contains(key)) data.put(key, old.get(key).copy());
        data.putBoolean(NBT_MEDITATING, false);
        data.putBoolean(NBT_CASTING, false);
        data.putBoolean(NBT_CAST_RELEASED, false);
        data.putBoolean(NBT_CAST_EFFECT_RELEASED, false);
        data.putInt(NBT_CAST_STAGE, 0);
        data.putBoolean(NBT_HAND_FORM, false);
        data.putBoolean(NBT_SCHEME_ACTIVE, false);
        data.putBoolean(NBT_BODY_PROTECTION, false);
        data.putBoolean(NBT_BODY_FORM_SELECTING, false);
        data.remove(NBT_BODY_FORM_ORIGINAL);
        data.remove(NBT_CAST_START);
        data.remove(NBT_CAST_ACTION_START);
        data.remove(NBT_CAST_LOCK_UNTIL);
        data.putBoolean("mjmod_control_fire_genius", old.getBoolean("mjmod_control_fire_genius"));
    }
}
