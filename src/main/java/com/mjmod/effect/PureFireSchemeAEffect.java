package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 纯质阳炎速度型（方案 A）。
 * 攻击伤害 +10%，移动速度 +40%。
 */
public final class PureFireSchemeAEffect extends MobEffect {
    public PureFireSchemeAEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF8A1F);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "f4e68737-0d1d-4fd8-a9d2-0e9f4fdccf01",
                0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "a1f546e8-31d0-47a0-9f4d-4e2bbdbd7f02",
                0.40D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}