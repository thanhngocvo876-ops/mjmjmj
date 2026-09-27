package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 纯质阳炎均衡型（方案 D）。
 * 攻击伤害 +30%，移动速度 +30%，受到的伤害降低 30%。
 * 所有数值均按百分比计算；减伤逻辑由 PureFireSkillManager 统一处理。
 */
public final class PureFireSchemeDEffect extends MobEffect {
    public PureFireSchemeDEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF3D12);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "6f2f8061-7ef3-4b29-a1d0-9b7a1d9f0d05",
                0.30D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "76e0c70f-0b2e-49f5-9f91-6be4a989e306",
                0.30D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
