package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Permanent cosmetic/social marker; relationship logic applies the penalties. */
public class UglyEffect extends MobEffect {
    public UglyEffect() { super(MobEffectCategory.HARMFUL, 0x51404A); }
}
