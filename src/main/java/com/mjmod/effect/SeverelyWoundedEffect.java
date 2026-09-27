package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** 虚空之泪命中的重伤：每秒造成 1.5 点魔法伤害。 */
public class SeverelyWoundedEffect extends MobEffect {
    public SeverelyWoundedEffect() {
        super(MobEffectCategory.HARMFUL, 0x7AA7D8);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.invulnerableTime = 0;
        entity.hurt(entity.damageSources().magic(), 1.5F);
        entity.invulnerableTime = 0;
    }
}
