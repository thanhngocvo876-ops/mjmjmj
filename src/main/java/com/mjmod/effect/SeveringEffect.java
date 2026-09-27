package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 泪刃命中的分割感：下半身失去移动能力，客户端同时隐藏双腿。 */
public class SeveringEffect extends MobEffect {
    public SeveringEffect() {
        super(MobEffectCategory.HARMFUL, 0xA9E9FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // Keep vertical motion for knockback/gravity, but remove all
            // horizontal movement so ordinary keys cannot move the lower body.
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);
            player.hurtMarked = true;
        }
    }
}
