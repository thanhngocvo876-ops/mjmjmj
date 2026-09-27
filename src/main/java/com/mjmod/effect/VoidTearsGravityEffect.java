package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚空之泪引力：强制削减水平速度并把目标轻微拉回命中轨道，普通行走
 * 很难脱离，只有高速移动或位移类技能才能明显挣脱。
 */
public class VoidTearsGravityEffect extends MobEffect {
    private static final Map<UUID, Vec3> ANCHORS = new ConcurrentHashMap<>();

    public VoidTearsGravityEffect() {
        super(MobEffectCategory.HARMFUL, 0x243B69);
    }

    public static void setAnchor(LivingEntity entity, Vec3 anchor) {
        if (entity != null && anchor != null) ANCHORS.put(entity.getUUID(), anchor);
    }

    public static void clearAnchor(LivingEntity entity) {
        if (entity != null) ANCHORS.remove(entity.getUUID());
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 anchor = ANCHORS.get(entity.getUUID());
        if (anchor == null) anchor = entity.position();

        Vec3 offset = anchor.subtract(entity.position());
        double distance = offset.length();
        Vec3 pull = distance > 0.05D
                ? offset.scale(Math.min(0.075D, 0.018D + distance * 0.004D) / distance)
                : Vec3.ZERO;

        // Dampen ordinary walking/knockback while retaining enough momentum for
        // sprint jumps, dashes and other genuine high-speed escape movement.
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.52D).add(pull));

        var effect = entity.getEffect(com.mjmod.registry.ModEffects.GRAVITY.get());
        if (effect == null || effect.getDuration() <= 1) {
            clearAnchor(entity);
        }
    }
}
