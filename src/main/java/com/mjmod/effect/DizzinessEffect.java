package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** 虚空之泪头部命中的头晕标记；黑幕由客户端渲染。 */
public class DizzinessEffect extends MobEffect {
    public DizzinessEffect() {
        super(MobEffectCategory.HARMFUL, 0x18243D);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Clear the active target every tick while the head-hit blackout is in
        // effect. The matching LivingChangeTargetEvent handler also prevents
        // vanilla AI from immediately reacquiring the same player.
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.setLastHurtByMob(null);
            mob.setLastHurtByPlayer(null);
        }
    }
}
