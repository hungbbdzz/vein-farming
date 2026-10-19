package com.velorise.veinfarming;

import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeinFarmingMod implements ModInitializer {
    public static final String MODID = "vein_farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        FarmingConfig.load();

        // Register custom networking payloads (C2S)
        PayloadTypeRegistry.playC2S().register(FarmingKeyPayload.TYPE, FarmingKeyPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleSmartPlantPayload.TYPE, ToggleSmartPlantPayload.STREAM_CODEC);

        // Server-side network receivers
        ServerPlayNetworking.registerGlobalReceiver(FarmingKeyPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                FarmingEventHandler.setPlayerKeyActive(context.player().getUUID(), payload.active());
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(ToggleSmartPlantPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                FarmingEventHandler.setPlayerSmartPlantEnabled(context.player().getUUID(), payload.enabled());
            });
        });

        // Register game event callbacks
        UseBlockCallback.EVENT.register(FarmingEventHandler::onRightClickBlock);
        PlayerBlockBreakEvents.BEFORE.register(FarmingEventHandler::onBlockBreak);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FarmingEventHandler.onPlayerLoggedOut(handler.getPlayer().getUUID());
        });

        preloadClasses();

        LOGGER.info("Vein Farming: Universal Crop Harvester (Fabric) initialized successfully!");
    }

    private static void preloadClasses() {
        Class<?>[] classes = new Class<?>[]{
                FarmingConfig.class,
                PlantClassifier.class,
                PlantingAlgorithms.class,
                HarvestManager.class,
                PlantingManager.class,
                TillingAndFertilizingManager.class,
                FarmingManager.class,
                ModAdvancements.class,
                LiteMinerCompat.class,
                FTBUltimineCompat.class,
                FarmingEventHandler.class
        };
        for (Class<?> clazz : classes) {
            try {
                Class.forName(clazz.getName(), true, VeinFarmingMod.class.getClassLoader());
            } catch (Throwable t) {
                LOGGER.warn("Vein Farming: Preloading class {} encountered note: {}", clazz.getName(), t.getMessage());
            }
        }
    }
}
