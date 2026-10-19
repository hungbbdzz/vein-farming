package com.velorise.veinfarming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FarmingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("vein_farming_config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ConfigValue<?>> ALL_VALUES = new ArrayList<>();

    public static final ConfigHolder SPEC = new ConfigHolder();

    // --- Primary Controls & Sliders ---
    public static final IntValue SAPLING_MIN_SPACING = defineInt("sapling_min_spacing", 4, 0, 8);
    public static final IntValue FLOWER_SPARSITY = defineInt("flower_sparsity", 3, 0, 5);
    public static final IntValue MAX_BLOCKS = defineInt("max_blocks", 91, 1, 512);
    public static final IntValue FARMING_RADIUS = defineInt("farming_radius", 8, 1, 32);
    public static final BooleanValue HARVEST_TO_INVENTORY = defineBool("harvest_to_inventory", false);
    public static final BooleanValue STANDALONE_PREVIEW = defineBool("standalone_preview", true);
    public static final BooleanValue SMART_MELON_PUMPKIN_PLANTING = defineBool("smart_melon_pumpkin_planting", true);
    public static final BooleanValue BEDROCK_FLOWER_BONEMEAL = defineBool("bedrock_flower_bonemeal", true);
    public static final BooleanValue SMART_INTERCROPPING = defineBool("smart_intercropping", true);
    public static final BooleanValue REPLANT_CROPS = defineBool("replant_crops", true);
    public static final BooleanValue PREVENT_TOOL_BREAKING = defineBool("prevent_tool_breaking", true);
    public static final BooleanValue CLEAR_FOLIAGE = defineBool("clear_foliage", true);
    public static final BooleanValue SMART_WATER_BUCKET_IRRIGATION = defineBool("smart_water_bucket_irrigation", true);
    public static final IntValue WATER_HOLE_SPACING = defineInt("water_hole_spacing", 8, 8, 16);
    public static final BooleanValue BATCH_COMPOSTER = defineBool("batch_composter", true);

    // --- Secondary & Advanced Settings ---
    public static final BooleanValue SMART_BONEMEAL = defineBool("smart_bonemeal", true);
    public static final BooleanValue PREVENT_FARMLAND_TRAMPLE = defineBool("prevent_farmland_trample", true);
    public static final BooleanValue DAMAGE_HOE_ON_HARVEST = defineBool("damage_hoe_on_harvest", true);
    public static final BooleanValue GHOST_PLANT_PREVIEW = defineBool("ghost_plant_preview", true);
    public static final BooleanValue GHOST_FARMLAND_PREVIEW = defineBool("ghost_farmland_preview", true);
    public static final BooleanValue SMART_IRRIGATION_PREVIEW = defineBool("smart_irrigation_preview", true);
    public static final BooleanValue GROWTH_PENALTY_WARNING = defineBool("growth_penalty_warning", true);
    public static final BooleanValue SATISFYING_AUDIO_CASCADE = defineBool("satisfying_audio_cascade", true);
    public static final DoubleValue EXHAUSTION_PER_BLOCK = defineDouble("exhaustion_per_block", 0.02, 0.0, 1.0);
    public static final BooleanValue COLLECT_DROPS_AT_TARGET = defineBool("collect_drops_at_target", true);
    public static final BooleanValue HARVEST_SUGAR_CANE = defineBool("harvest_sugar_cane", true);
    public static final BooleanValue ENABLE_MASS_HARVEST = defineBool("enable_mass_harvest", true);
    public static final BooleanValue PULL_FROM_INVENTORY = defineBool("pull_from_inventory", true);
    public static final BooleanValue SMART_SAPLING_2X2 = defineBool("smart_sapling_2x2", true);

    // Legacy fields kept for backward compatibility
    public static final BooleanValue SMART_SAPLING_PLANTING = defineBool("smart_sapling_planting", true);
    public static final BooleanValue SMART_FLOWER_PLANTING = defineBool("smart_flower_planting", true);

    public static boolean isSmartSaplingEnabled() {
        return SAPLING_MIN_SPACING.get() > 0;
    }

    public static boolean isSmartFlowerEnabled() {
        return FLOWER_SPARSITY.get() > 0;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("vein_farming.json");
    }

    public static void load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (ConfigValue<?> val : ALL_VALUES) {
                if (json.has(val.getKey())) {
                    val.loadFromJson(json);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load vein_farming.json config, saving defaults", e);
            save();
        }
    }

    public static void save() {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject json = new JsonObject();
            for (ConfigValue<?> val : ALL_VALUES) {
                val.writeToJson(json);
            }
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save vein_farming.json config", e);
        }
    }

    private static IntValue defineInt(String key, int def, int min, int max) {
        IntValue v = new IntValue(key, def, min, max);
        ALL_VALUES.add(v);
        return v;
    }

    private static BooleanValue defineBool(String key, boolean def) {
        BooleanValue v = new BooleanValue(key, def);
        ALL_VALUES.add(v);
        return v;
    }

    private static DoubleValue defineDouble(String key, double def, double min, double max) {
        DoubleValue v = new DoubleValue(key, def, min, max);
        ALL_VALUES.add(v);
        return v;
    }

    public static class ConfigHolder {
        public void save() {
            FarmingConfig.save();
        }
    }

    public static abstract class ConfigValue<T> {
        protected final String key;
        protected final T defaultValue;
        protected T value;

        public ConfigValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public T get() {
            return value;
        }

        public void set(T val) {
            this.value = val;
        }

        public T getDefault() {
            return defaultValue;
        }

        public abstract void loadFromJson(JsonObject json);
        public abstract void writeToJson(JsonObject json);
    }

    public static class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        public IntValue(String key, int defaultValue, int min, int max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Integer val) {
            super.set(Math.max(min, Math.min(max, val)));
        }

        @Override
        public void loadFromJson(JsonObject json) {
            if (json.has(key)) {
                set(json.get(key).getAsInt());
            }
        }

        @Override
        public void writeToJson(JsonObject json) {
            json.addProperty(key, value);
        }
    }

    public static class BooleanValue extends ConfigValue<Boolean> {
        public BooleanValue(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public void loadFromJson(JsonObject json) {
            if (json.has(key)) {
                set(json.get(key).getAsBoolean());
            }
        }

        @Override
        public void writeToJson(JsonObject json) {
            json.addProperty(key, value);
        }
    }

    public static class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        public DoubleValue(String key, double defaultValue, double min, double max) {
            super(key, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        public void set(Double val) {
            super.set(Math.max(min, Math.min(max, val)));
        }

        @Override
        public void loadFromJson(JsonObject json) {
            if (json.has(key)) {
                set(json.get(key).getAsDouble());
            }
        }

        @Override
        public void writeToJson(JsonObject json) {
            json.addProperty(key, value);
        }
    }
}
