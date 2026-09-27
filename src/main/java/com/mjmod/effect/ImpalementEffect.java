package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 专属穿刺状态（Impalement）：
 * 目标移动能力归零，无法还击，持续时间精确对齐穿刺动作完全结束。
 */
public class ImpalementEffect extends MobEffect {
    public ImpalementEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF4500);
    }
}
