package com.helicoptermod.entity;

import com.helicoptermod.HelicopterMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Entity type registry for this mod.
 * <p>
 * There is deliberately only ONE entry here: {@link #HELICOPTER}. This mod adds exactly one
 * vehicle, on purpose — do not add more EntityTypes here without updating the renderer/model too.
 */
public final class ModEntities {

    public static final EntityType<HelicopterEntity> HELICOPTER = register(
            "helicopter",
            EntityType.Builder.<HelicopterEntity>create(HelicopterEntity::new, SpawnGroup.MISC)
                    // Hitbox only — the visual model is intentionally a bit larger (rotor span etc.),
                    // which is normal for Minecraft vehicles (boats work the same way).
                    .dimensions(1.6f, 1.8f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(1)
                    .makeFireImmune()
    );

    private static EntityType<HelicopterEntity> register(String path, EntityType.Builder<HelicopterEntity> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(HelicopterMod.MOD_ID, path));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    /** Forces this class to load (and therefore its static registrations to run) at a controlled time. */
    public static void init() {
    }

    private ModEntities() {
    }
}
