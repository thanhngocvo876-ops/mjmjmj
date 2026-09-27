package com.mjmod.skill;

import com.mjmod.network.MJNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 雷法加速（斯安维斯坦）。
 *
 * 残影照闪电侠源码的客户端假实体：走路把位置存进队列，延迟后再画，
 * 残影本身碰不到、不挡路。时间放缓照 JOJO 时停源码的范围判定，
 * 但只减速到 1/4，不整服停表，也不搬 1.12 / Mixin 代码。
 */
public final class ThunderAccelSkillManager {
    public static final int DURATION_TICKS = 100 * 20;
    public static final int COOLDOWN_TICKS = 20 * 20;
    public static final double RANGE = 100.0D;
    /** 周围生物和弹射物每 8 拍只放行 1 拍，比原来的 1/4 再慢一倍。 */
    public static final int SLOW_STRIDE = 8;

    private static final String ROOT = "mjmod_thunder_accel";
    private static final String ACTIVE = "active";
    private static final String REMAINING = "remaining";
    private static final String COOLDOWN = "cooldown";

    private static final Map<UUID, Vec3> STORED_MOTION = new HashMap<>();
    private static final Map<UUID, Integer> SLOW_PHASE = new HashMap<>();
    private static final UUID SLOW_SPEED_ID =
            UUID.fromString("6c1a9e20-7b44-4d1e-9a55-0f3c8b2e71aa");
    private static final UUID SLOW_ATTACK_ID =
            UUID.fromString("91d4c0b7-2e58-4a6f-b813-5c7e0a9d44f2");

    private ThunderAccelSkillManager() {}

    public static boolean isActive(Player player) {
        CompoundTag tag = tag(player);
        return tag.getBoolean(ACTIVE) && tag.getInt(REMAINING) > 0;
    }

    public static int remainingTicks(Player player) {
        return Math.max(0, tag(player).getInt(REMAINING));
    }

    public static int cooldownTicks(Player player) {
        return Math.max(0, tag(player).getInt(COOLDOWN));
    }

    public static void toggle(ServerPlayer player) {
        if (isActive(player)) {
            stop(player, true);
            return;
        }
        int cooldown = cooldownTicks(player);
        if (cooldown > 0) {
            player.displayClientMessage(Component.literal(
                    "§c雷法加速冷却中，还剩 " + ((cooldown + 19) / 20) + " 秒。"), true);
            return;
        }
        CompoundTag tag = tag(player);
        tag.putBoolean(ACTIVE, true);
        tag.putInt(REMAINING, DURATION_TICKS);
        save(player, tag);
        player.displayClientMessage(Component.literal("§b雷法加速开启，持续 100 秒。"), true);
        MJNetwork.sendThunderAccelStatus(player);
    }

    public static void tick(ServerPlayer player) {
        CompoundTag tag = tag(player);
        boolean changed = false;
        if (tag.getInt(COOLDOWN) > 0) {
            tag.putInt(COOLDOWN, tag.getInt(COOLDOWN) - 1);
            changed = true;
        }
        if (tag.getBoolean(ACTIVE)) {
            int remaining = tag.getInt(REMAINING) - 1;
            if (remaining <= 0 || !player.isAlive()) {
                tag.putBoolean(ACTIVE, false);
                tag.putInt(REMAINING, 0);
                if (player.isAlive()) {
                    tag.putInt(COOLDOWN, COOLDOWN_TICKS);
                    player.displayClientMessage(Component.literal("§7雷法加速结束，冷却 20 秒。"), true);
                }
            } else {
                tag.putInt(REMAINING, remaining);
            }
            changed = true;
        }
        if (changed) {
            save(player, tag);
            if (player.tickCount % 5 == 0 || !tag.getBoolean(ACTIVE)) {
                MJNetwork.sendThunderAccelStatus(player);
            }
        }
    }

    /** 生物 tick 开头。范围内每 4 拍取消 3 拍，动作、AI、攻击一起变成大约 1/4。 */
    public static boolean shouldFreezeTick(LivingEntity living) {
        if (living instanceof Player player && isActive(player)) return false;
        if (!(living.level() instanceof ServerLevel level)) return false;
        var users = level.players().stream().filter(ThunderAccelSkillManager::isActive).toList();
        if (users.isEmpty() || !nearActiveUser(living, users)) {
            restore(living);
            return false;
        }
        UUID id = living.getUUID();
        int phase = SLOW_PHASE.merge(id, 1, Integer::sum);
        applySlowAttribute(living.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_SPEED_ID, "mjmod_thunder_slow_move");
        applySlowAttribute(living.getAttribute(Attributes.ATTACK_SPEED), SLOW_ATTACK_ID, "mjmod_thunder_slow_attack");
        if (!living.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                || living.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() < 10) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 7, false, false, false));
        }
        boolean freeze = phase % SLOW_STRIDE != 0;
        if (freeze) {
            living.setSprinting(false);
            living.xxa = 0.0F;
            living.zza = 0.0F;
            living.setDeltaMovement(0.0D, living.getDeltaMovement().y, 0.0D);
        }
        return freeze;
    }

    /** 弹射物没有 LivingTick，在世界 tick 开头按同样的 1/4 节拍放慢。 */
    public static void tickProjectiles(ServerLevel level) {
        var users = level.players().stream().filter(ThunderAccelSkillManager::isActive).toList();
        if (users.isEmpty()) {
            restoreAll(level);
            return;
        }
        AABB union = null;
        for (ServerPlayer user : users) {
            AABB box = user.getBoundingBox().inflate(RANGE);
            union = union == null ? box : union.minmax(box);
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, union)) {
            if (!nearActiveUser(projectile, users)) {
                restore(projectile);
                continue;
            }
            slowProjectile(projectile);
        }
    }

    public static boolean shouldCancelIncomingHit(LivingEntity victim, Entity attacker) {
        if (!(victim instanceof Player victimPlayer) || !isActive(victimPlayer)) return false;
        if (attacker instanceof Player attackerPlayer && isActive(attackerPlayer)) return false;
        return true;
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(ROOT);
        MJNetwork.sendThunderAccelStatus(player);
    }

    private static void stop(ServerPlayer player, boolean startCooldown) {
        CompoundTag tag = tag(player);
        tag.putBoolean(ACTIVE, false);
        tag.putInt(REMAINING, 0);
        if (startCooldown) tag.putInt(COOLDOWN, COOLDOWN_TICKS);
        save(player, tag);
        player.displayClientMessage(Component.literal(
                startCooldown ? "§7雷法加速已关闭，冷却 20 秒。" : "§7雷法加速已关闭。"), true);
        MJNetwork.sendThunderAccelStatus(player);
    }

    private static boolean nearActiveUser(Entity entity, java.util.List<ServerPlayer> users) {
        for (ServerPlayer user : users) {
            if (entity.distanceToSqr(user) <= RANGE * RANGE) return true;
        }
        return false;
    }

    private static void slowProjectile(Entity projectile) {
        UUID id = projectile.getUUID();
        Vec3 stored = STORED_MOTION.get(id);
        if (stored == null || stored.lengthSqr() < 1.0E-6D) {
            stored = projectile.getDeltaMovement();
            STORED_MOTION.put(id, stored);
        }
        int phase = SLOW_PHASE.merge(id, 1, Integer::sum);
        if (phase % SLOW_STRIDE == 0) {
            projectile.setDeltaMovement(stored);
        } else {
            projectile.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void restore(Entity entity) {
        Vec3 stored = STORED_MOTION.remove(entity.getUUID());
        SLOW_PHASE.remove(entity.getUUID());
        if (stored != null && entity instanceof Projectile) {
            entity.setDeltaMovement(stored);
        }
        if (entity instanceof LivingEntity living) {
            removeSlowAttribute(living.getAttribute(Attributes.MOVEMENT_SPEED), SLOW_SPEED_ID);
            removeSlowAttribute(living.getAttribute(Attributes.ATTACK_SPEED), SLOW_ATTACK_ID);
            MobEffectInstance slow = living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slow != null && !slow.isVisible() && slow.getDuration() <= 15) {
                living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    private static void applySlowAttribute(AttributeInstance attribute, UUID id, String name) {
        if (attribute == null || attribute.getModifier(id) != null) return;
        attribute.addTransientModifier(new AttributeModifier(
                id, name, -0.875D, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeSlowAttribute(AttributeInstance attribute, UUID id) {
        if (attribute != null && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    private static void restoreAll(ServerLevel level) {
        if (STORED_MOTION.isEmpty() && SLOW_PHASE.isEmpty()) return;
        for (Entity entity : level.getAllEntities()) {
            if (STORED_MOTION.containsKey(entity.getUUID()) || SLOW_PHASE.containsKey(entity.getUUID())) {
                restore(entity);
            }
        }
    }

    private static CompoundTag tag(Player player) {
        CompoundTag root = player.getPersistentData();
        return root.contains(ROOT) ? root.getCompound(ROOT) : new CompoundTag();
    }

    private static void save(Player player, CompoundTag tag) {
        player.getPersistentData().put(ROOT, tag);
    }
}
