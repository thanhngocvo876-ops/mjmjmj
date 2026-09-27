package com.mjmod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 七、八技能结束后留在右手上的火刃标记。效果本身不改属性，
 * 同步给观察者后由手上渲染层画刃，近战命中由服务端补重伤。
 */
public final class PureFireBladeHoldEffect extends MobEffect {
    public PureFireBladeHoldEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6A1A);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
