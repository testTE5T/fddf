package com.helicoptermod.item;

import com.helicoptermod.HelicopterMod;
import com.helicoptermod.entity.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Item registry for this mod. Only one item: the placeable {@link #HELICOPTER}.
 */
public final class ModItems {

    public static final HelicopterItem HELICOPTER = register(
            "helicopter",
            new HelicopterItem(ModEntities.HELICOPTER, new Item.Settings().maxCount(1))
    );

    private static <T extends Item> T register(String path, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(HelicopterMod.MOD_ID, path), item);
    }

    public static void init() {
        // Show it in the creative inventory under Tools & Utilities, next to boats/minecarts/saddles.
        // Change ItemGroups.TOOLS below if you'd rather it live in a different tab.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(HELICOPTER));
    }

    private ModItems() {
    }
}
