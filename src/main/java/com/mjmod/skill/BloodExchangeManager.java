package com.mjmod.skill;

import com.mjmod.entity.OrientalClanEntity;
import com.mjmod.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative tracking, immobilisation and execution for 换血功. */
public final class BloodExchangeManager {
    private BloodExchangeManager() {}
    private static final int DURATION = 60;
    private static final double RANGE = 16.0D;
    private static final DustParticleOptions BLOOD =
            new DustParticleOptions(new Vector3f(0.72F, 0.015F, 0.035F), 1.25F);
    private static final Map<UUID, Exchange> ACTIVE = new ConcurrentHashMap<>();

    private record Exchange(UUID target, int ticks) {}

    /** Returns true when Z was consumed by the blood technique path. */
    public static boolean tryCastFromKey(ServerPlayer caster) {
        if (!RelationshipManager.hasBloodTechnique(caster)) {
            caster.sendSystemMessage(Component.literal("§c你尚未学会换血功。"));
            return false;
        }
        if (ACTIVE.containsKey(caster.getUUID())) return true;
        LivingEntity target = findLookTarget(caster);
        if (target == null) {
            caster.sendSystemMessage(Component.literal(
                    "§c请在 16 格内用准心锁定低血量东方一族或拥有东方神血的玩家。"));
            return false;
        }
        if (target instanceof OrientalClanEntity && target.getHealth() > 5.0F) {
            caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c东方一族必须只剩 5 点生命值才能施展换血功。"));
            return true;
        }
        if (!(target instanceof OrientalClanEntity)) {
            if (!(target instanceof ServerPlayer targetPlayer)
                    || !RelationshipManager.hasDivineBloodPermission(targetPlayer)) {
                caster.sendSystemMessage(Component.literal(
                        "§c该目标体内没有可被抽取的东方神血。"));
                return false;
            }
        }
        begin(caster, target);
        return true;
    }

    private static LivingEntity findLookTarget(ServerPlayer caster) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getViewVector(1.0F).normalize().scale(RANGE));
        var blockHit = caster.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB search = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        var hit = ProjectileUtil.getEntityHitResult(caster, start, end, search,
                entity -> entity instanceof LivingEntity living
                        && living != caster && living.isAlive()
                        && !living.isSpectator() && living.isPickable(),
                start.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static void begin(ServerPlayer caster, LivingEntity target) {
        ACTIVE.put(caster.getUUID(), new Exchange(target.getUUID(), DURATION));
        lock(caster);
        lock(target);
        caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§d换血功启动：血脉锁定，持续抽取东方神血……"));
    }

    private static void lock(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION, 254,
                false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 254,
                false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATION, 254,
                false, false, true));
    }

    public static void tick(ServerPlayer caster) {
        Exchange exchange = ACTIVE.get(caster.getUUID());
        if (exchange == null) return;
        Entity found = caster.serverLevel().getEntity(exchange.target());
        if (found == null) found = caster.server.getPlayerList().getPlayer(exchange.target());
        if (!(found instanceof LivingEntity target) || !target.isAlive()) {
            ACTIVE.remove(caster.getUUID());
            unlock(caster);
            return;
        }
        lock(caster);
        lock(target);
        ServerLevel level = caster.serverLevel();
        bloodParticles(level, caster.position());
        bloodParticles((ServerLevel) target.level(), target.position());
        if (target.level() == level && leftLineParticleTick(exchange.ticks())) {
            bloodLink(level, caster.getEyePosition(), target.getEyePosition());
        }
        int left = exchange.ticks() - 1;
        if (left <= 0) {
            ACTIVE.remove(caster.getUUID());
            unlock(caster);
            unlock(target);
            target.setHealth(0.0F);
            target.die(target.damageSources().magic());
            RelationshipManager.grantBloodTechnique(caster, false);
            RelationshipManager.grantDivineBloodPermission(caster);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0,
                    target.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
            caster.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§d换血功完成：东方神血已融入你的体内。"));
        } else {
            ACTIVE.put(caster.getUUID(), new Exchange(exchange.target(), left));
        }
    }

    private static void bloodParticles(ServerLevel level, Vec3 pos) {
        level.sendParticles(BLOOD, pos.x, pos.y + 1.0, pos.z,
                7, 0.45, 0.75, 0.45, 0.015);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y + 1.0, pos.z,
                2, 0.25, 0.45, 0.25, 0.02);
    }

    private static boolean leftLineParticleTick(int ticks) {
        return (ticks & 1) == 0;
    }

    private static void bloodLink(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int points = Math.max(4, (int) Math.ceil(delta.length() * 2.5D));
        for (int i = 1; i < points; i++) {
            Vec3 point = start.add(delta.scale(i / (double) points));
            level.sendParticles(BLOOD, point.x, point.y, point.z,
                    1, 0.035, 0.035, 0.035, 0.0);
        }
    }

    private static void unlock(LivingEntity entity) {
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.WEAKNESS);
    }
}
