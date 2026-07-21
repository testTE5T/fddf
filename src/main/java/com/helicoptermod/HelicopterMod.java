package com.helicoptermod;

import com.helicoptermod.entity.ModEntities;
import com.helicoptermod.item.ModItems;
import com.helicoptermod.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Runs on both the client and the dedicated server.
 * <p>
 * This mod intentionally registers just three things: one sound event, one entity type,
 * and one item (the placeable "Helicopter" item that spawns that entity). Nothing else.
 */
public class HelicopterMod implements ModInitializer {

    public static final String MOD_ID = "helicoptermod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Order matters: sounds first (no dependencies), then the entity type,
        // then the item (which references the entity type it places).
        ModSounds.init();
        ModEntities.init();
        ModItems.init();

        LOGGER.info("Helicopter Mod ready: one light transport helicopter, no fuel, no weapons, easy to fly.");
    }
}
