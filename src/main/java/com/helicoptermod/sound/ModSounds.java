package com.helicoptermod.sound;

import com.helicoptermod.HelicopterMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Sound event registry for this mod. Only one sound: the looping engine hum, played client-side
 * while a helicopter is being piloted (see {@code HelicopterModClient} and
 * {@code HelicopterEngineSoundInstance}).
 */
public final class ModSounds {

    public static final SoundEvent HELICOPTER_ENGINE = register("helicopter_engine");

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(HelicopterMod.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
    }

    private ModSounds() {
    }
}
