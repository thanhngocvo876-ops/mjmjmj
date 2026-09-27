package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 纯阳护体的同步标记。实际减伤在服务端 LivingHurtEvent 中按百分比
 * 计算，避免把固定护甲点数误当作百分比减伤。
 */
public final class PureYangBodyProtectionEffect extends MobEffect {
    public PureYangBodyProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFF0A0);
    }
}
