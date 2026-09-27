package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Server-authoritative marker for the second pure-fire skill. The hidden
 * effect synchronizes to observing clients so the hand flames render for
 * everyone, while the attribute modifier raises all normal melee damage by
 * exactly fifty percent.
 */
public final class PureFireHandFormEffect extends MobEffect {
    public PureFireHandFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD35A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "2d404c83-aab8-4c37-b572-4749eb633f4f",
                0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
