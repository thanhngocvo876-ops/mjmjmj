package com.mjmod.skill;

import com.mjmod.entity.VoidTearBeamEntity;
import com.mjmod.entity.VoidTearShieldEntity;
import com.mjmod.entity.VoidTearBladeEntity;
import com.mjmod.entity.VoidTearDropEntity;
import com.mjmod.registry.ModEffects;
import com.mjmod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 独立的虚空之泪技能系统。 */
public final class VoidTearsSkillManager {
    private VoidTearsSkillManager() {}

    /** Whether the player has currently revealed the Void Tears model. */
    public static final String NBT_VOID_TEARS_ACTIVE = "mjmod_void_tears_active";

    public static final int SKILL_BEAMS = 1;
    public static final int SKILL_SHIELD = 2;
    public static final int SKILL_TELEPORT = 3;
    public static final int SKILL_BLADE = 4;
    public static final int SKILL_DROPS = 5;
    /** Compatibility alias for callers that used the old empty-slot name. */
    public static final int SKILL_EMPTY = SKILL_SHIELD;
    private static final String NBT_COOLDOWN_UNTIL = "mjmod_void_tears_cooldown_until";
    /** Permanent permission granted when the consumable Void Tears item is used. */
    public static final String NBT_VOID_TEARS_PERMISSION = "mjmod_void_tears_permission";
    public static final String NBT_ACTIVE_UNTIL = "mjmod_void_tears_active_until";
    public static final String NBT_COORDINATE_POINTS = "mjmod_void_tears_coordinate_points";
    public static final String NBT_SELECTED_POINT = "mjmod_void_tears_selected_point";
    private static final int MAX_COORDINATE_POINTS = 32;
    private static final double TELEPORT_RANGE = 35.0D;
    /** Per-skill cooldowns, in game ticks. The old shared key remains as a
     * legacy prefix so existing worlds do not need data migration. */
    private static final long BEAMS_COOLDOWN_TICKS = 50L;       // 2.5 s
    private static final long SHIELD_NORMAL_COOLDOWN_TICKS = 300L; // 15 s
    private static final long SHIELD_SHIFT_COOLDOWN_TICKS = 100L;  // 5 s
    private static final long TELEPORT_NORMAL_COOLDOWN_TICKS = 40L; // 2 s
    private static final long TELEPORT_SHIFT_COOLDOWN_TICKS = 400L; // 20 s
    private static final long BLADE_COOLDOWN_TICKS = 400L;      // 20 s
    private static final long DROP_GUARD_COOLDOWN_TICKS = 60L;  // 3 s
    public static final long ACTIVE_DURATION_TICKS = 3L * 60L * 20L;
    private static final String NBT_DROP_NEXT_SPAWN = "mjmod_void_tears_drop_next_spawn";
    private static final String NBT_DROP_VOLLEY_READY = "mjmod_void_tears_drop_volley_ready";
    private static final String NBT_DROP_AUTOMATION_ENABLED = "mjmod_void_tears_drop_automation_enabled";
    /** True only after the player explicitly casts skill 5. */
    private static final String NBT_DROP_GUARD_EXPLICIT = "mjmod_void_tears_drop_guard_explicit";
    /** 悲情之心数值，单位为百分比；不会被上限截断。 */
    public static final String NBT_TRAGIC_HEART = "mjmod_void_tears_tragic_heart";
    private static final float DROP_BEAM_DAMAGE = 25.0F;
    public static final int MAX_TEAR_DROPS = 6;
    private static final int DROP_SPAWN_INTERVAL = 40;

    private static String cooldownKey(int skill, boolean shift) {
        return NBT_COOLDOWN_UNTIL + "_" + skill + (shift ? "_shift" : "");
    }

    private static long cooldownTicks(int skill, boolean shift) {
        return switch (skill) {
            case SKILL_BEAMS -> BEAMS_COOLDOWN_TICKS;
            case SKILL_SHIELD -> shift ? SHIELD_SHIFT_COOLDOWN_TICKS
                    : SHIELD_NORMAL_COOLDOWN_TICKS;
            case SKILL_TELEPORT -> shift ? TELEPORT_SHIFT_COOLDOWN_TICKS
                    : TELEPORT_NORMAL_COOLDOWN_TICKS;
            case SKILL_BLADE -> BLADE_COOLDOWN_TICKS;
            case SKILL_DROPS -> DROP_GUARD_COOLDOWN_TICKS;
            default -> 0L;
        };
    }

    private static boolean isOnCooldown(ServerPlayer player, int skill, boolean shift) {
        long now = player.serverLevel().getGameTime();
        long until = player.getPersistentData().getLong(cooldownKey(skill, shift));
        if (now >= until) return false;
        double seconds = (until - now) / 20.0D;
        String formatted = String.format(java.util.Locale.ROOT, "%.1f", seconds);
        player.sendSystemMessage(Component.literal(
                "\u00a7c虚空之泪技能冷却中，还需 " + formatted + " 秒。"));
        return true;
    }

    private static void startCooldown(ServerPlayer player, int skill, boolean shift) {
        long ticks = cooldownTicks(skill, shift);
        if (ticks <= 0L) return;
        player.getPersistentData().putLong(cooldownKey(skill, shift),
                player.serverLevel().getGameTime() + ticks);
    }

    /** A saved cross-dimensional teleport destination shown in the point UI. */
    public record CoordinatePoint(String uid, String dimension,
                                  double x, double y, double z) {}

    /**
     * Whether the player has permanently unlocked the Void Tears artifact.
     * This is deliberately independent from both the inventory and armor slots:
     * consuming the item grants the permission, and the permission survives
     * after the item is gone.
     */
    public static boolean hasPermission(ServerPlayer player) {
        return player.getPersistentData().getBoolean(NBT_VOID_TEARS_PERMISSION);
    }

    public static int get悲情之心(ServerPlayer player) {
        return Math.max(0, player.getPersistentData().getInt(NBT_TRAGIC_HEART));
    }

    /** 仅供命令或管理工具设置，不提供常规掉落/获取途径。 */
    public static void set悲情之心(ServerPlayer player, int value) {
        int clamped = Math.max(0, value);
        player.getPersistentData().putInt(NBT_TRAGIC_HEART, clamped);
        awardVoidTearsAdvancements(player, getVoidTearsLevel(player));
        com.mjmod.network.MJNetwork.sendVoidTearsData(player);
    }

    /** 四级：拥有权限即 LV1，之后每 25% 心值提升一级；100% 后继续无限叠加伤害。 */
    public static int getVoidTearsLevel(ServerPlayer player) {
        if (!hasPermission(player)) return 0;
        return Math.min(4, 1 + get悲情之心(player) / 25);
    }

    public static float get悲情之心伤害加成(ServerPlayer player) {
        return player == null ? 0.0F : get悲情之心(player) / 25 * 5.0F;
    }

    private static final ResourceLocation[] VOID_TEARS_ADVANCEMENTS = new ResourceLocation[] {
            null,
            new ResourceLocation("mjmod", "void_tears/level_1"),
            new ResourceLocation("mjmod", "void_tears/level_2"),
            new ResourceLocation("mjmod", "void_tears/level_3"),
            new ResourceLocation("mjmod", "void_tears/level_4")
    };

    public static void awardVoidTearsAdvancements(ServerPlayer player, int level) {
        for (int i = 1; i <= Math.min(4, Math.max(0, level)); i++) {
            Advancement advancement = player.server.getAdvancements().getAdvancement(VOID_TEARS_ADVANCEMENTS[i]);
            if (advancement != null) player.getAdvancements().award(advancement, "unlocked");
        }
    }

    /** Compatibility helper for older callers and old worlds that still have
     * the pre-conversion item. New skill code must use {@link #hasPermission}. */
    public static boolean hasVoidTearsItem(ServerPlayer player) {
        return hasPermission(player)
                || player.getInventory().contains(new ItemStack(ModItems.VOID_TEARS_HELMET.get()));
    }

    /**
     * One-time migration for worlds created with the old armor implementation.
     * A legacy copy left in the helmet slot cannot be right-clicked as an item,
     * so convert that exact slot to the new permanent permission and clear it.
     */
    public static void migrateLegacyEquippedItem(ServerPlayer player) {
        if (hasPermission(player)) return;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.VOID_TEARS_HELMET.get())) return;

        player.getPersistentData().putBoolean(NBT_VOID_TEARS_PERMISSION, true);
        awardVoidTearsAdvancements(player, 1);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        // Do not carry the old armor-triggered three-minute state into the new
        // flow; the player must explicitly use an onion to make it visible.
        player.getPersistentData().putBoolean(NBT_VOID_TEARS_ACTIVE, false);
        player.getPersistentData().remove(NBT_ACTIVE_UNTIL);
        com.mjmod.network.MJNetwork.sendVoidTearsActive(player, false);
        com.mjmod.network.MJNetwork.sendVoidTearsData(player);
        player.sendSystemMessage(Component.literal(
                "\u00a7b旧版头部护甲已转换为虚空之泪法宝权限。\u00a7r\u00a77 使用洋葱可显形。"));
    }

    /** Kept as a source-compatibility alias for older callers. */
    @Deprecated
    public static boolean hasHelmet(ServerPlayer player) {
        return hasPermission(player);
    }

    private static long serverTime(ServerPlayer player) {
        return player.serverLevel().getServer().overworld().getGameTime();
    }

    public static boolean isActive(ServerPlayer player) {
        // An active flag from an older build must not keep the cosmetic layer
        // visible or allow casts after the old armor/item-based permission was
        // removed. The player can recover by using the new consumable once.
        if (!hasPermission(player)) {
            if (player.getPersistentData().getBoolean(NBT_VOID_TEARS_ACTIVE)) {
                deactivate(player, false);
            }
            return false;
        }
        if (!player.getPersistentData().getBoolean(NBT_VOID_TEARS_ACTIVE)) {
            return false;
        }
        long until = player.getPersistentData().getLong(NBT_ACTIVE_UNTIL);
        if (until <= serverTime(player)) {
            deactivate(player, true);
            return false;
        }
        return true;
    }

    public static void activate(ServerPlayer player) {
        if (!hasPermission(player)) {
            return;
        }
        player.getPersistentData().putBoolean(NBT_VOID_TEARS_ACTIVE, true);
        player.getPersistentData().putLong(NBT_ACTIVE_UNTIL,
                serverTime(player) + ACTIVE_DURATION_TICKS);
        player.getPersistentData().putLong(NBT_DROP_NEXT_SPAWN,
                player.serverLevel().getGameTime() + DROP_SPAWN_INTERVAL);
        player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, false);
        // Onion activation opens the three-minute skill window, but it must not
        // automatically cast the separate Void Tears bodyguard skill.
        player.getPersistentData().putBoolean(NBT_DROP_AUTOMATION_ENABLED, false);
        player.getPersistentData().putBoolean(NBT_DROP_GUARD_EXPLICIT, false);
        clearTearDrops(player);
        com.mjmod.network.MJNetwork.sendVoidTearsActive(player, true);
    }

    public static long getRemainingActiveTicks(ServerPlayer player) {
        return Math.max(0L, player.getPersistentData().getLong(NBT_ACTIVE_UNTIL) - serverTime(player));
    }

    public static void tickActivation(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(NBT_VOID_TEARS_ACTIVE)) {
            isActive(player);
        }
    }

    /** 自动化第五技能：激活且选中技能 5 时每 2 秒生成一滴，最多 6 滴。 */
    public static void tickTearDropAutomation(ServerPlayer player) {
        // The bead reserve is passive: it keeps forming while the artifact is
        // active, even when another Void Tears skill is selected.
        if (!isActive(player) || !isDropAutomationEnabled(player)) return;
        trimTearDropsToLimit(player);
        if (!player.getPersistentData().getBoolean(NBT_DROP_VOLLEY_READY)
                && countTearDrops(player) >= MAX_TEAR_DROPS) {
            player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, true);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b虚空水珠已凝聚至 6 滴，Z 可逐滴发射激光。"));
        }
        // A full volley is a fixed set of six waiting beads. Do not create
        // replacements midway through it; this keeps every Z press consuming
        // exactly one of the visible beads and prevents stale forming states.
        if (player.getPersistentData().getBoolean(NBT_DROP_VOLLEY_READY)) return;
        long now = player.serverLevel().getGameTime();
        long next = player.getPersistentData().getLong(NBT_DROP_NEXT_SPAWN);
        if (next <= 0L) {
            next = now + DROP_SPAWN_INTERVAL;
            player.getPersistentData().putLong(NBT_DROP_NEXT_SPAWN, next);
        }
        if (now < next) return;
        player.getPersistentData().putLong(NBT_DROP_NEXT_SPAWN, now + DROP_SPAWN_INTERVAL);
        int count = countTearDrops(player);
        if (count < MAX_TEAR_DROPS) {
            spawnTearDrop(player);
        }
    }

    private static int getSelectedSkill(ServerPlayer player) {
        int selected = player.getPersistentData().getInt("mjmod_void_tears_skill");
        return selected >= SKILL_BEAMS && selected <= SKILL_DROPS ? selected : SKILL_BEAMS;
    }

    private static boolean isDropAutomationEnabled(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        // Old saves can contain ENABLED=true from the former automatic onion
        // path. Requiring the new explicit marker migrates them to disabled.
        return data.getBoolean(NBT_DROP_GUARD_EXPLICIT)
                && data.getBoolean(NBT_DROP_AUTOMATION_ENABLED);
    }

    /** Normal Z on skill 5 explicitly starts the guard and bead formation. */
    private static void startTearDropAutomation(ServerPlayer player) {
        if (!hasPermission(player) || !isActive(player)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c虚空之泪尚未激活，无法切换第五技能。"));
            return;
        }
        player.getPersistentData().putBoolean(NBT_DROP_GUARD_EXPLICIT, true);
        player.getPersistentData().putBoolean(NBT_DROP_AUTOMATION_ENABLED, true);
        player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, false);
        player.getPersistentData().putLong(NBT_DROP_NEXT_SPAWN,
                player.serverLevel().getGameTime() + DROP_SPAWN_INTERVAL);
        player.sendSystemMessage(Component.literal(
                "\u00a7b虚空之泪护体已开启：虚空水珠将每 2 秒生成。"));
    }

    /** Shift+Z on skill 5 only closes the guard and recalls all beads. */
    private static void stopTearDropAutomation(ServerPlayer player) {
        player.getPersistentData().putBoolean(NBT_DROP_GUARD_EXPLICIT, true);
        player.getPersistentData().putBoolean(NBT_DROP_AUTOMATION_ENABLED, false);
        player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, false);
        player.getPersistentData().remove(NBT_DROP_NEXT_SPAWN);
        clearTearDrops(player);
        player.sendSystemMessage(Component.literal(
                "\u00a77虚空之泪护体已关闭：现有虚空水珠已收回。"));
    }

    private static void clearTearDrops(ServerPlayer player) {
        for (VoidTearDropEntity drop : player.serverLevel().getEntitiesOfClass(
                VoidTearDropEntity.class, player.getBoundingBox().inflate(128.0D),
                candidate -> candidate.isOwnedBy(player))) {
            drop.discard();
        }
    }

    private static int countTearDrops(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(VoidTearDropEntity.class,
                player.getBoundingBox().inflate(128.0D),
                drop -> drop.isOwnedBy(player)).size();
    }

    /** Removes beads left over from older worlds so the live cap is always six. */
    private static void trimTearDropsToLimit(ServerPlayer player) {
        List<VoidTearDropEntity> drops = player.serverLevel().getEntitiesOfClass(
                VoidTearDropEntity.class, player.getBoundingBox().inflate(128.0D),
                drop -> drop.isOwnedBy(player));
        if (drops.size() <= MAX_TEAR_DROPS) return;
        drops.sort(java.util.Comparator.comparingInt(Entity::getId));
        for (int i = MAX_TEAR_DROPS; i < drops.size(); i++) {
            drops.get(i).discard();
        }
    }

    private static void spawnTearDrop(ServerPlayer player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(view).normalize();
        Vec3 origin = eye.add(view.scale(0.32D))
                .add(right.scale(0.13D)).add(up.scale(0.04D));

        // Keep the random rear formation inside a two-block lateral band.
        double behind = 0.80D + player.getRandom().nextDouble() * 0.75D;
        double lateral = (player.getRandom().nextDouble() * 3.8D) - 1.9D;
        double height = 0.25D + player.getRandom().nextDouble() * 2.15D;
        VoidTearDropEntity drop = new VoidTearDropEntity(
                player.serverLevel(), player, origin, behind, lateral, height);
        player.serverLevel().addFreshEntity(drop);
    }

    private enum TearDropBeamResult { LAUNCHED, NOT_READY }

    /** Z on skill 5 emits a first-skill laser from one bead without consuming it. */
    private static TearDropBeamResult launchNextTearDrop(ServerPlayer player) {
        List<VoidTearDropEntity> idle = player.serverLevel().getEntitiesOfClass(
                VoidTearDropEntity.class, player.getBoundingBox().inflate(128.0D),
                drop -> drop.isOwnedBy(player) && drop.isIdle());
        if (!player.getPersistentData().getBoolean(NBT_DROP_VOLLEY_READY)
                && idle.size() >= MAX_TEAR_DROPS) {
            player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, true);
        }
        if (!player.getPersistentData().getBoolean(NBT_DROP_VOLLEY_READY)
                || idle.isEmpty()) return TearDropBeamResult.NOT_READY;

        VoidTearDropEntity next = idle.stream()
                .sorted(java.util.Comparator.comparingInt(Entity::getId))
                .skip(Math.floorMod(player.getPersistentData()
                        .getInt("mjmod_void_tears_drop_beam_source"), idle.size()))
                .findFirst().orElse(null);
        if (next == null) return TearDropBeamResult.NOT_READY;

        Vec3 playerFacing = player.getViewVector(1.0F).normalize();
        spawnVoidTearBeam(player, next.position(), playerFacing, DROP_BEAM_DAMAGE);
        startCooldown(player, SKILL_DROPS, false);
        int source = player.getPersistentData()
                .getInt("mjmod_void_tears_drop_beam_source");
        player.getPersistentData().putInt("mjmod_void_tears_drop_beam_source", source + 1);
        com.mjmod.network.MJNetwork.sendSkillAnimation(player, 10, 12);
        return TearDropBeamResult.LAUNCHED;
    }

    /** When an enemy hurts the owner, one waiting drop immediately retaliates. */
    public static void triggerTearDropDefense(ServerPlayer player, Entity attacker) {
        if (!(attacker instanceof LivingEntity target) || target == player
                || !target.isAlive() || !isActive(player)
                || !isDropAutomationEnabled(player)) return;

        // Self-defence now emits a laser without consuming any bead. It only
        // exists while at least one visible bead is available, so the passive
        // is naturally disabled when the reserve is empty.
        Vec3 source = player.serverLevel().getEntitiesOfClass(
                        VoidTearDropEntity.class, player.getBoundingBox().inflate(128.0D),
                        drop -> drop.isOwnedBy(player) && drop.isIdle())
                .stream().min(java.util.Comparator.comparingDouble(
                        drop -> drop.distanceToSqr(target)))
                .map(Entity::position)
                .orElse(null);
        if (source == null) return;
        if (source.distanceToSqr(target.position())
                > VoidTearDropEntity.MAX_DISTANCE * VoidTearDropEntity.MAX_DISTANCE) return;
        if (isOnCooldown(player, SKILL_DROPS, false)) return;
        Vec3 direction = target.getBoundingBox().getCenter().subtract(source);
        if (direction.lengthSqr() < 1.0E-6D) return;
        spawnVoidTearBeam(player, source, direction.normalize(), DROP_BEAM_DAMAGE);
        startCooldown(player, SKILL_DROPS, false);
    }

    private static void spawnVoidTearBeam(ServerPlayer player, Vec3 origin, Vec3 direction) {
        spawnVoidTearBeam(player, origin, direction, 10.0F);
    }

    private static void spawnVoidTearBeam(ServerPlayer player, Vec3 origin,
                                           Vec3 direction, float damage) {
        VoidTearBeamEntity beam = new VoidTearBeamEntity(
                player.serverLevel(), player, origin, direction, damage);
        player.serverLevel().addFreshEntity(beam);
    }

    private static void deactivate(ServerPlayer player, boolean notify) {
        player.getPersistentData().putBoolean(NBT_VOID_TEARS_ACTIVE, false);
        player.getPersistentData().remove(NBT_ACTIVE_UNTIL);
        player.getPersistentData().remove(NBT_DROP_NEXT_SPAWN);
        player.getPersistentData().putBoolean(NBT_DROP_VOLLEY_READY, false);
        player.getPersistentData().putBoolean(NBT_DROP_AUTOMATION_ENABLED, false);
        player.getPersistentData().putBoolean(NBT_DROP_GUARD_EXPLICIT, false);
        clearTearDrops(player);
        com.mjmod.network.MJNetwork.sendVoidTearsActive(player, false);
        if (notify) {
            player.sendSystemMessage(Component.literal(
                    "\u00a77洋葱效果已经消失，虚空之泪恢复为未激活状态。"));
        }
    }

    public static void switchSkill(ServerPlayer player, int selectedSkill) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("\u00a7c需要先使用虚空之泪消耗品获得法宝权限。"));
            return;
        }
        int skill = selectedSkill >= SKILL_BEAMS && selectedSkill <= SKILL_DROPS
                ? selectedSkill : SKILL_BEAMS;
        player.getPersistentData().putInt("mjmod_void_tears_skill", skill);
        if (skill == SKILL_DROPS) {
            // Selecting the slot is not a cast. Keep the guard dormant until
            // the player presses the release key on skill 5.
            player.getPersistentData().putLong(NBT_DROP_NEXT_SPAWN,
                    player.serverLevel().getGameTime() + DROP_SPAWN_INTERVAL);
        }
        String label = switch (skill) {
            case SKILL_BEAMS -> "三道泪流";
            case SKILL_SHIELD -> "水盾·万泪齐发";
            case SKILL_TELEPORT -> "削减空间·坐标传送";
            case SKILL_BLADE -> "泪刃·竖劈";
            case SKILL_DROPS -> "虚空水珠·自动护卫";
            default -> "未知";
        };
        player.sendSystemMessage(Component.literal("\u00a7b虚空之泪技能已切换：" + label));
    }

    public static void cast(ServerPlayer player, int selectedSkill) {
        cast(player, selectedSkill, false);
    }

    /**
     * Releases a Void Tears skill. Shift+Z on skill 2 selects the defensive
     * water-shield extension; the normal skill-2 branch remains unchanged.
     */
    public static void cast(ServerPlayer player, int selectedSkill, boolean shift) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal("\u00a7c需要先使用虚空之泪消耗品获得法宝权限。"));
            return;
        }
        if (!isActive(player)) {
            player.sendSystemMessage(Component.literal("\u00a7c虚空之泪尚未激活，请先用洋葱右键激活。"));
            return;
        }
        if (selectedSkill == SKILL_DROPS && shift) {
            stopTearDropAutomation(player);
            return;
        }
        if (selectedSkill == SKILL_SHIELD) {
            if (isOnCooldown(player, SKILL_SHIELD, shift)) return;
            VoidTearShieldEntity shield = new VoidTearShieldEntity(
                    player.serverLevel(), player, player.getViewVector(1.0F).normalize(), shift);
            player.serverLevel().addFreshEntity(shield);
            startCooldown(player, SKILL_SHIELD, shift);
            player.sendSystemMessage(Component.literal(shift
                    ? "\u00a7b虚空之泪：防御水盾已展开（持续 3 秒，吸收上限 500 点）！"
                    : "\u00a7b虚空之泪：水盾蓄力中（3 秒后万泪齐发）！"));
            com.mjmod.network.MJNetwork.sendSkillAnimation(player, 7, VoidTearShieldEntity.CHARGE_TICKS);
            return;
        }

        if (selectedSkill == SKILL_DROPS) {
            if (!isDropAutomationEnabled(player)) {
                if (isOnCooldown(player, SKILL_DROPS, false)) return;
                startTearDropAutomation(player);
                startCooldown(player, SKILL_DROPS, false);
                return;
            }
            if (isOnCooldown(player, SKILL_DROPS, false)) return;
            TearDropBeamResult result = launchNextTearDrop(player);
            if (result == TearDropBeamResult.NOT_READY) {
                player.sendSystemMessage(Component.literal(
                        "\u00a77虚空水珠尚未达到 6 滴；满额后 Z 可逐颗发射激光。"));
            }
            return;
        }

        if (selectedSkill == SKILL_TELEPORT) {
            if (shift) {
                // The client opens the coordinate form for Shift+Z. Keep a
                // server-side guard for forged/old packets so the former UID
                // point behaviour can never silently trigger a teleport.
                player.sendSystemMessage(Component.literal(
                        "\u00a7b请在虚空之泪传送界面填写坐标后确认传送。"));
                return;
            }

            if (isOnCooldown(player, SKILL_TELEPORT, false)) return;

            Vec3 destination = findCrosshairDestination(player);
            if (destination == null) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c准心范围内没有可传送的目标或位置。"));
                return;
            }
            boolean upwardCrosshairTeleport = player.getXRot() < 0.0F
                    && destination.y > player.getY() + 0.5D;
            teleportWithWaterEffect(player, player.serverLevel(), destination,
                    player.getYRot(), player.getXRot(), upwardCrosshairTeleport);
            startCooldown(player, SKILL_TELEPORT, false);
            com.mjmod.network.MJNetwork.sendSkillAnimation(player, 8, 12);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b虚空之泪：空间削减，传送完成。"));
            return;
        }

        if (selectedSkill == SKILL_BLADE) {
            if (isOnCooldown(player, SKILL_BLADE, false)) return;
            Vec3 view = player.getViewVector(1.0F).normalize();
            Vec3 eye = player.getEyePosition(1.0F);
            Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
            else right = right.normalize();
            Vec3 up = right.cross(view).normalize();
            // The blade is born at the visible right-eye artifact and then
            // travels straight toward the player's designated crosshair.
            Vec3 origin = eye.add(view.scale(0.34D))
                    .add(right.scale(0.13D)).add(up.scale(0.04D));
            VoidTearBladeEntity blade = new VoidTearBladeEntity(
                    player.serverLevel(), player, origin, view);
            player.serverLevel().addFreshEntity(blade);
            startCooldown(player, SKILL_BLADE, false);
            // A blade cast consumes the current onion activation. Permission
            // remains permanent, but another onion is required before any
            // Void Tears skill can be used again.
            deactivate(player, false);
            com.mjmod.network.MJNetwork.sendSkillAnimation(player, 9,
                    VoidTearBladeEntity.CHARGE_TICKS + VoidTearBladeEntity.GROW_TICKS
                            + VoidTearBladeEntity.FADE_TICKS);
            player.sendSystemMessage(Component.literal(
                    "\u00a7b虚空之泪：泪刃成形，竖劈已释放。\u00a77 请再次使用洋葱激活法宝。"));
            return;
        }

        if (selectedSkill != SKILL_BEAMS) {
            player.sendSystemMessage(Component.literal("\u00a77虚空之泪技能槽无效。"));
            return;
        }

        if (isOnCooldown(player, SKILL_BEAMS, false)) return;

        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 right = view.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 headUp = right.cross(view).normalize();
        // The earlier fixed upward angle is intentionally removed: the beam
        // follows the player's crosshair exactly, including vertical aim.
        Vec3 direction = view;

        // The authored helmet core is about 2 px toward the player's right eye,
        // 4 px in front of the head and slightly above the camera eye line.
        Vec3 tearAnchor = eye.add(view.scale(0.31D))
                .add(right.scale(0.13D))
                .add(headUp.scale(0.04D));
        for (int i = -1; i <= 1; i++) {
            // Keep the three rails close together, but separated enough that
            // their individual cores remain visible in first person.
            Vec3 origin = tearAnchor.add(right.scale(i * 0.105D));
            Vec3 beamDirection = direction.add(right.scale(i * 0.012D)).normalize();
            VoidTearBeamEntity beam = new VoidTearBeamEntity(
                    player.serverLevel(), player, origin, beamDirection);
            player.serverLevel().addFreshEntity(beam);
        }
        startCooldown(player, SKILL_BEAMS, false);
        com.mjmod.network.MJNetwork.sendSkillAnimation(player, 6, 16);
        player.sendSystemMessage(Component.literal("\u00a7b虚空之泪：三道泪流！"));
    }

    /**
     * Server-authoritative teleport submitted by the coordinate screen. The
     * dimension is resolved through the normal registry, so dimensions added by
     * other mods work without a hard-coded list.
     */
    public static boolean teleportToCoordinates(ServerPlayer player, String dimension,
                                                double x, double y, double z,
                                                boolean emergency, boolean shiftCast) {
        if (!hasPermission(player)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c需要先使用虚空之泪消耗品获得法宝权限。"));
            return false;
        }
        if (!isActive(player)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c虚空之泪尚未激活，请先用洋葱右键激活。"));
            return false;
        }

        if (isOnCooldown(player, SKILL_TELEPORT, shiftCast)) return false;

        ServerLevel destination;
        Vec3 destinationPos;
        if (emergency) {
            destination = player.serverLevel();
            destinationPos = findEmergencyPosition(destination, player);
            if (destinationPos == null) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c紧急传送未找到安全位置，请稍后再试。"));
                return false;
            }
        } else {
            if (dimension == null || dimension.isBlank()
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                player.sendSystemMessage(Component.literal("\u00a7c传送坐标或维度格式无效。"));
                return false;
            }
            ResourceLocation location = ResourceLocation.tryParse(dimension.trim());
            if (location == null) {
                player.sendSystemMessage(Component.literal("\u00a7c维度 ID 无效，请填写例如 minecraft:overworld。"));
                return false;
            }
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
            destination = player.getServer().getLevel(dimensionKey);
            if (destination == null) {
                player.sendSystemMessage(Component.literal(
                        "\u00a7c找不到目标维度：" + dimension.trim()));
                return false;
            }
            double clampedY = Math.max(destination.getMinBuildHeight() + 1.0D,
                    Math.min(destination.getMaxBuildHeight() - 2.0D, y));
            destinationPos = new Vec3(x, clampedY, z);
        }

        teleportWithWaterEffect(player, destination, destinationPos,
                player.getYRot(), player.getXRot(), false);
        startCooldown(player, SKILL_TELEPORT, shiftCast);
        com.mjmod.network.MJNetwork.sendSkillAnimation(player, 8, 12);
        player.sendSystemMessage(Component.literal(emergency
                ? "\u00a7b虚空之泪：紧急传送完成（约 200 格外）。"
                : "\u00a7b虚空之泪：坐标传送完成。"));
        return true;
    }

    private static Vec3 findEmergencyPosition(ServerLevel level, ServerPlayer player) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            int blockX = Mth.floor(player.getX() + Math.cos(angle) * 200.0D);
            int blockZ = Mth.floor(player.getZ() + Math.sin(angle) * 200.0D);
            int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    blockX, blockZ);
            Vec3 candidate = new Vec3(blockX + 0.5D, height + 0.05D, blockZ + 0.5D);
            AABB moved = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (level.noCollision(player, moved)) return candidate;
        }
        return null;
    }

    /** Returns an immutable snapshot of all saved teleport points. */
    public static List<CoordinatePoint> getCoordinatePoints(ServerPlayer player) {
        List<CoordinatePoint> points = new ArrayList<>();
        ListTag list = player.getPersistentData().getList(NBT_COORDINATE_POINTS,
                Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CoordinatePoint point = readCoordinatePoint(list.getCompound(i));
            if (point != null) points.add(point);
        }
        return Collections.unmodifiableList(points);
    }

    public static String getSelectedPointUid(ServerPlayer player) {
        return player.getPersistentData().getString(NBT_SELECTED_POINT);
    }

    public static Optional<CoordinatePoint> getSelectedCoordinatePoint(ServerPlayer player) {
        String selected = getSelectedPointUid(player);
        if (selected.isEmpty()) return Optional.empty();
        for (CoordinatePoint point : getCoordinatePoints(player)) {
            if (point.uid().equals(selected)) return Optional.of(point);
        }
        // Clear stale selections left by an old/deleted point.
        player.getPersistentData().remove(NBT_SELECTED_POINT);
        return Optional.empty();
    }

    public static CoordinatePoint addCoordinatePoint(ServerPlayer player) {
        ListTag list = player.getPersistentData().getList(NBT_COORDINATE_POINTS,
                Tag.TAG_COMPOUND);
        if (list.size() >= MAX_COORDINATE_POINTS) return null;

        CompoundTag tag = new CompoundTag();
        tag.putString("Uid", UUID.randomUUID().toString());
        tag.putString("Dimension", player.serverLevel().dimension().location().toString());
        tag.putDouble("X", player.getX());
        tag.putDouble("Y", player.getY());
        tag.putDouble("Z", player.getZ());
        list.add(tag);
        player.getPersistentData().put(NBT_COORDINATE_POINTS, list);
        return readCoordinatePoint(tag);
    }

    public static boolean selectCoordinatePoint(ServerPlayer player, String uid) {
        if (uid == null || uid.isBlank()) {
            player.getPersistentData().remove(NBT_SELECTED_POINT);
            return true;
        }
        for (CoordinatePoint point : getCoordinatePoints(player)) {
            if (point.uid().equals(uid)) {
                player.getPersistentData().putString(NBT_SELECTED_POINT, uid);
                return true;
            }
        }
        return false;
    }

    public static boolean deleteCoordinatePoint(ServerPlayer player, String uid) {
        if (uid == null || uid.isBlank()) return false;
        ListTag list = player.getPersistentData().getList(NBT_COORDINATE_POINTS,
                Tag.TAG_COMPOUND);
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (uid.equals(list.getCompound(i).getString("Uid"))) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            player.getPersistentData().put(NBT_COORDINATE_POINTS, list);
            if (uid.equals(getSelectedPointUid(player))) {
                player.getPersistentData().remove(NBT_SELECTED_POINT);
            }
        }
        return removed;
    }

    public static boolean teleportToSelectedPoint(ServerPlayer player) {
        Optional<CoordinatePoint> selected = getSelectedCoordinatePoint(player);
        if (selected.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c请先在坐标点界面选择一个传送点。"));
            return false;
        }
        CoordinatePoint point = selected.get();
        ResourceLocation location = ResourceLocation.tryParse(point.dimension());
        if (location == null) {
            player.sendSystemMessage(Component.literal("\u00a7c坐标点维度数据无效。"));
            return false;
        }
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, location);
        ServerLevel destination = player.getServer().getLevel(dimensionKey);
        if (destination == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7c目标维度当前不可用：" + point.dimension()));
            return false;
        }

        double y = Math.max(destination.getMinBuildHeight() + 1.0D,
                Math.min(destination.getMaxBuildHeight() - 2.0D, point.y()));
        teleportWithWaterEffect(player, destination,
                new Vec3(point.x(), y, point.z()), player.getYRot(), player.getXRot(), false);
        return true;
    }

    /** Finds the nearest living entity or block position under the crosshair. */
    private static Vec3 findCrosshairDestination(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 maxEnd = start.add(view.scale(TELEPORT_RANGE));
        HitResult blockHit = level.clip(new ClipContext(start, maxEnd,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double reach = blockHit.getType() == HitResult.Type.MISS
                ? TELEPORT_RANGE : start.distanceTo(blockHit.getLocation());
        Vec3 end = start.add(view.scale(reach));

        LivingEntity nearest = null;
        Vec3 nearestHit = null;
        double nearestDistance = reach * reach;
        AABB search = new AABB(start, end).inflate(1.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3> hit = target.getBoundingBox().inflate(0.25D).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = target;
                nearestHit = hit.get();
            }
        }

        if (nearest != null) {
            // Put the player just above the target's feet to avoid embedding in
            // its collision box while still arriving at the aimed destination.
            return nearest.position().add(0.0D, 0.05D, 0.0D);
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return blockHit.getLocation().subtract(view.scale(0.65D));
        }
        return end.subtract(view.scale(0.65D));
    }

    private static void teleportWithWaterEffect(ServerPlayer player, ServerLevel destination,
                                                Vec3 position, float yaw, float pitch,
                                                boolean grantUpwardResistance) {
        ServerLevel origin = player.serverLevel();
        spawnWaterParticles(origin, player.position());

        // 传送时清掉旧的坠落距离并保留横向速度，避免把传送前的下坠
        // 动量带到新位置。这里不再给予缓降效果。
        player.fallDistance = 0.0F;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0.0D, velocity.z);

        player.teleportTo(destination, position.x, position.y, position.z, yaw, pitch);
        player.fallDistance = 0.0F;
        // 只有普通三技能在准心朝上、目标点确实更高时才获得 2 秒抗性 V。
        // 坐标传送、紧急传送、水平及向下传送均不会触发该 Buff。
        if (grantUpwardResistance) {
            player.addEffect(new MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    40, 4, false, false, true));
        }
        velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, 0.0D, velocity.z);
        spawnWaterParticles(destination, player.position());
    }

    private static void spawnWaterParticles(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.SPLASH, position.x, position.y + 0.9D,
                position.z, 8, 0.28D, 0.55D, 0.28D, 0.05D);
        level.sendParticles(ParticleTypes.FALLING_WATER, position.x, position.y + 1.0D,
                position.z, 4, 0.22D, 0.5D, 0.22D, 0.02D);
    }

    private static CoordinatePoint readCoordinatePoint(CompoundTag tag) {
        String uid = tag.getString("Uid");
        String dimension = tag.getString("Dimension");
        if (uid.isEmpty() || dimension.isEmpty()) return null;
        return new CoordinatePoint(uid, dimension, tag.getDouble("X"),
                tag.getDouble("Y"), tag.getDouble("Z"));
    }

    /** Shared hit handling for every future Void Tears skill. */
    public static float applyVoidTearHit(ServerLevel level, LivingEntity attacker,
                                         LivingEntity target, HitResult hit, float baseDamage) {
        boolean headHit = isHeadHit(target, hit);
        float damage = baseDamage + (attacker instanceof ServerPlayer sp
                ? get悲情之心伤害加成(sp) : 0.0F);
        if (headHit) damage *= 2.0F;
        // Void Tears damage ignores vanilla's hurt-resistance window. This is
        // shared here so the beam, shield shots, and all future skills behave
        // identically.
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().indirectMagic(attacker, attacker), damage);
        target.invulnerableTime = 0;

        target.addEffect(new MobEffectInstance(ModEffects.SEVERELY_WOUNDED.get(),
                200, 0, false, true, true));
        com.mjmod.effect.VoidTearsGravityEffect.setAnchor(target, hit.getLocation());
        target.addEffect(new MobEffectInstance(ModEffects.GRAVITY.get(),
                200, 0, false, true, true));
        // Slowness IV makes ordinary walking ineffective; the custom gravity
        // effect still preserves enough momentum for sprinting and dashes.
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                200, 4, false, true, true));

        if (headHit) {
            target.addEffect(new MobEffectInstance(ModEffects.DIZZINESS.get(),
                100, 0, false, true, true));
            target.addEffect(new MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.BLINDNESS,
                    100, 0, false, false, false));
            clearMobAggro(target);
            target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,
                100, 2, false, true, true));
        }
        return damage;
    }

    private static void clearMobAggro(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.setLastHurtByMob(null);
            mob.setLastHurtByPlayer(null);
        }
    }

    /** Treat the upper part of any entity's collision box as its head. */
    public static boolean isHeadHit(LivingEntity target, HitResult hit) {
        if (!(hit instanceof EntityHitResult entityHit) || entityHit.getEntity() != target) {
            return false;
        }
        double headThreshold = Math.max(0.35D, target.getBbHeight() * 0.22D);
        return hit.getLocation().y >= target.getBoundingBox().maxY - headThreshold;
    }

    /** Copies persistent Void Tears and relationship-independent state on respawn. */
    public static void copyPlayerData(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        CompoundTag oldData = oldPlayer.getPersistentData();
        CompoundTag newData = newPlayer.getPersistentData();
        if (oldData.contains(NBT_VOID_TEARS_PERMISSION)) {
            newData.putBoolean(NBT_VOID_TEARS_PERMISSION,
                    oldData.getBoolean(NBT_VOID_TEARS_PERMISSION));
        }
        if (oldData.contains(NBT_TRAGIC_HEART)) {
            newData.putInt(NBT_TRAGIC_HEART, oldData.getInt(NBT_TRAGIC_HEART));
        }
        if (oldData.contains(NBT_COORDINATE_POINTS, Tag.TAG_LIST)) {
            newData.put(NBT_COORDINATE_POINTS,
                    oldData.getList(NBT_COORDINATE_POINTS, Tag.TAG_COMPOUND).copy());
        }
        if (oldData.contains(NBT_SELECTED_POINT, Tag.TAG_STRING)) {
            newData.putString(NBT_SELECTED_POINT, oldData.getString(NBT_SELECTED_POINT));
        }
        // The model is always hidden after death; onion activation is required again.
        newData.putBoolean(NBT_VOID_TEARS_ACTIVE, false);
        newData.remove(NBT_ACTIVE_UNTIL);
        newData.remove(NBT_DROP_AUTOMATION_ENABLED);
        newData.remove(NBT_DROP_GUARD_EXPLICIT);
        newData.remove(NBT_DROP_VOLLEY_READY);
        newData.remove(NBT_DROP_NEXT_SPAWN);
    }
}
