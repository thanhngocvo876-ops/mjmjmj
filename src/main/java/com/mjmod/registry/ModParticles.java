package com.mjmod.registry;

import com.mjmod.MJMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom particle types used by the pure-fire visual effects. */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MJMod.MODID);

    /** A bright, tapered ember with a realistic fire colour gradient. */
    public static final RegistryObject<SimpleParticleType> PURE_FIRE =
            PARTICLES.register("pure_fire", () -> new SimpleParticleType(false));

    /** Five-frame DragonMineZ-inspired aura mask, tinted by the client spawner. */
    public static final RegistryObject<SimpleParticleType> PURE_FIRE_AURA =
            PARTICLES.register("pure_fire_aura", () -> new SimpleParticleType(true));

    /** Inner hot-core sparks hugging the body during the pure-fire power-up. */
    public static final RegistryObject<SimpleParticleType> PURE_FIRE_AURA_CORE =
            PARTICLES.register("pure_fire_aura_core", () -> new SimpleParticleType(true));
}
