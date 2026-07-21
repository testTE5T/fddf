package com.helicoptermod.client;

import com.helicoptermod.HelicopterMod;
import com.helicoptermod.entity.HelicopterEntity;
import com.helicoptermod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.TypeFilter;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-only entrypoint: registers the model layer + renderer for the helicopter, and drives the
 * looping engine sound (all client-only sound classes live under this {@code client} package so
 * they never get touched on a dedicated server — see {@code HelicopterEntity}, which is safe to
 * load on both sides because it never references any of this).
 */
public class HelicopterModClient implements ClientModInitializer {

    public static final EntityModelLayer HELICOPTER_MODEL_LAYER =
            new EntityModelLayer(Identifier.of(HelicopterMod.MOD_ID, "helicopter"), "main");

    private final Map<Integer, HelicopterEngineSoundInstance> engineSounds = new HashMap<>();

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(HELICOPTER_MODEL_LAYER, HelicopterEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.HELICOPTER, HelicopterEntityRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (world == null) {
            this.engineSounds.clear();
            return;
        }

        for (HelicopterEntity helicopter : world.getEntitiesByType(TypeFilter.instanceOf(HelicopterEntity.class), e -> true)) {
            boolean flying = helicopter.getControllingPassenger() != null;
            HelicopterEngineSoundInstance existing = this.engineSounds.get(helicopter.getId());

            if (flying && (existing == null || existing.isDone())) {
                HelicopterEngineSoundInstance instance = new HelicopterEngineSoundInstance(helicopter);
                this.engineSounds.put(helicopter.getId(), instance);
                client.getSoundManager().play(instance);
            } else if (!flying && existing != null) {
                this.engineSounds.remove(helicopter.getId());
            }
        }

        this.engineSounds.values().removeIf(HelicopterEngineSoundInstance::isDone);
    }
}
