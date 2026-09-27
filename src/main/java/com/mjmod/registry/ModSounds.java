package com.mjmod.registry;

import com.mjmod.MJMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MJMod.MODID);

    public static final RegistryObject<SoundEvent> LEECH_DIALOGUE =
            SOUNDS.register("leech_dialogue", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(MJMod.MODID, "leech_dialogue")));
}
