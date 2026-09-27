package com.mjmod.registry;

import com.mjmod.MJMod;
import com.mjmod.effect.DizzinessEffect;
import com.mjmod.effect.SeverelyWoundedEffect;
import com.mjmod.effect.VoidTearsGravityEffect;
import com.mjmod.effect.SeveringEffect;
import com.mjmod.effect.HandsomeEffect;
import com.mjmod.effect.UglyEffect;
import com.mjmod.effect.PureFireBladeHoldEffect;
import com.mjmod.effect.PureFireHandFormEffect;
import com.mjmod.effect.PureFireSchemeAEffect;
import com.mjmod.effect.PureFireSchemeBEffect;
import com.mjmod.effect.PureFireSchemeDEffect;
import com.mjmod.effect.PureYangBodyProtectionEffect;
import com.mjmod.effect.ImpalementEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MJMod.MODID);

    public static final RegistryObject<MobEffect> SEVERELY_WOUNDED =
            EFFECTS.register("severely_wounded", SeverelyWoundedEffect::new);
    public static final RegistryObject<MobEffect> DIZZINESS =
            EFFECTS.register("dizziness", DizzinessEffect::new);
    public static final RegistryObject<MobEffect> GRAVITY =
            EFFECTS.register("void_tears_gravity", VoidTearsGravityEffect::new);
    public static final RegistryObject<MobEffect> SEVERING =
            EFFECTS.register("void_tears_severing", SeveringEffect::new);
    public static final RegistryObject<MobEffect> HANDSOME =
            EFFECTS.register("handsome", HandsomeEffect::new);
    public static final RegistryObject<MobEffect> UGLY =
            EFFECTS.register("ugly", UglyEffect::new);
    public static final RegistryObject<MobEffect> PURE_FIRE_HAND_FORM =
            EFFECTS.register("pure_fire_hand_form", PureFireHandFormEffect::new);
    public static final RegistryObject<MobEffect> PURE_FIRE_BLADE_HOLD =
            EFFECTS.register("pure_fire_blade_hold", PureFireBladeHoldEffect::new);
    public static final RegistryObject<MobEffect> PURE_FIRE_SCHEME_A =
            EFFECTS.register("pure_fire_scheme_a", PureFireSchemeAEffect::new);
    public static final RegistryObject<MobEffect> PURE_FIRE_SCHEME_B =
            EFFECTS.register("pure_fire_scheme_b", PureFireSchemeBEffect::new);
    public static final RegistryObject<MobEffect> PURE_FIRE_SCHEME_D =
            EFFECTS.register("pure_fire_scheme_d", PureFireSchemeDEffect::new);
    public static final RegistryObject<MobEffect> PURE_YANG_BODY_PROTECTION =
            EFFECTS.register("pure_yang_body_protection", PureYangBodyProtectionEffect::new);
    public static final RegistryObject<MobEffect> IMPALEMENT =
            EFFECTS.register("impalement", ImpalementEffect::new);
}
