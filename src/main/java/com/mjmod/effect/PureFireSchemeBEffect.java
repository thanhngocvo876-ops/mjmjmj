package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 纯质阳炎防御型（方案 B）。
 * 攻击伤害 +10%，移动速度 -20%，受到的伤害降低 40%。
 * 减伤逻辑由 PureFireSkillManager 统一处理。
 */
public final class PureFireSchemeBEffect extends MobEffect {
    public PureFireSchemeBEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFC247);
        addAttributeModifier(Attributes.ATTACK_DAMAGE,
                "c2bb3b3d-b41a-42db-bb50-9f5c4f18d303",
                0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                "fa1d9c42-0bf8-4f92-97f2-2f7183f2b304",
                -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
