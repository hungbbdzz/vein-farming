package com.velorise.veinfarming;

import com.velorise.veinfarming.client.ClientEventHandler;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Mod(VeinFarmingMod.MODID)
public class VeinFarmingMod {
    public static final String MODID = "vein_farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public VeinFarmingMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FarmingConfig.SPEC);
        registerPayloads();

        MinecraftForge.EVENT_BUS.register(new FarmingEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventHandler.initClient();
        }

        preloadClasses();

        LOGGER.info("Vein Farming: Universal Crop Harvester (Forge 1.20.1) initialized successfully!");
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

    private static void registerPayloads() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                FarmingKeyPayload.class,
                FarmingKeyPayload::encode,
                FarmingKeyPayload::decode,
                FarmingKeyPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id++,
                ToggleSmartPlantPayload.class,
                ToggleSmartPlantPayload::encode,
                ToggleSmartPlantPayload::decode,
                ToggleSmartPlantPayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
