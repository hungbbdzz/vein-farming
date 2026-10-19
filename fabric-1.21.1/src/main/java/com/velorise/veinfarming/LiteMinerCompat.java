package com.velorise.veinfarming;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LiteMinerCompat {
    public static final String LITEMINER_MODID = "liteminer";

    public static boolean isLiteMinerLoaded() {
        return FabricLoader.getInstance().isModLoaded(LITEMINER_MODID);
    }

    public static boolean isLiteMinerActive(ServerPlayer player) {
        if (!isLiteMinerLoaded() || player == null) {
            return false;
        }
        try {
            Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
            Object instance = liteminerClass.getField("instance").get(null);
            if (instance != null) {
                Method getPlayerStateMethod = instance.getClass().getMethod("getPlayerState", ServerPlayer.class);
                Object state = getPlayerStateMethod.invoke(instance, player);
                if (state != null) {
                    Method getKeymappingState = state.getClass().getMethod("getKeymappingState");
                    return (boolean) getKeymappingState.invoke(state);
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isLiteMinerClientActive() {
        if (!isLiteMinerLoaded()) {
            return false;
        }
        try {
            Class<?> clientClass = Class.forName("com.iamkaf.liteminer.LiteminerClient");
            Method isVeinMiningMethod = clientClass.getMethod("isVeinMining");
            if ((boolean) isVeinMiningMethod.invoke(null)) {
                return true;
            }
            Field keyField = clientClass.getField("KEY_MAPPING");
            net.minecraft.client.KeyMapping keyMapping = (net.minecraft.client.KeyMapping) keyField.get(null);
            return keyMapping != null && keyMapping.isDown();
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Collection<BlockPos> getClientSelectedBlocks(Level level, Player player, BlockPos targetPos) {
        if (!isLiteMinerLoaded()) {
            return Collections.emptyList();
        }
        try {
            Class<?> clientClass = Class.forName("com.iamkaf.liteminer.LiteminerClient");
            Field selectedBlocksField = clientClass.getField("selectedBlocks");
            Collection<?> set = (Collection<?>) selectedBlocksField.get(null);
            if (set != null && !set.isEmpty()) {
                List<BlockPos> list = new ArrayList<>();
                for (Object o : set) {
                    if (o instanceof BlockPos bp) list.add(bp);
                }
                return list;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static Collection<BlockPos> getSelectedBlocks(ServerPlayer player, BlockPos targetPos) {
        if (!isLiteMinerLoaded() || player == null || targetPos == null) {
            return Collections.emptyList();
        }
        try {
            Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
            Object instance = liteminerClass.getField("instance").get(null);
            if (instance != null) {
                Method getPlayerStateMethod = instance.getClass().getMethod("getPlayerState", ServerPlayer.class);
                Object state = getPlayerStateMethod.invoke(instance, player);
                if (state != null) {
                    Method getShapeMethod = state.getClass().getMethod("getShape");
                    int shapeIndex = (int) getShapeMethod.invoke(state);
                    Field walkersField = liteminerClass.getField("WALKERS");
                    List<?> walkers = (List<?>) walkersField.get(null);
                    if (shapeIndex >= 0 && shapeIndex < walkers.size()) {
                        Object walker = walkers.get(shapeIndex);
                        Method walkMethod = walker.getClass().getMethod("walk", Level.class, Player.class, BlockPos.class);
                        Object walked = walkMethod.invoke(walker, player.level(), player, targetPos);
                        if (walked instanceof Collection<?> col) {
                            List<BlockPos> list = new ArrayList<>();
                            for (Object o : col) {
                                if (o instanceof BlockPos bp) list.add(bp);
                            }
                            return list;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static int getEffectiveBlockLimit() {
        if (isLiteMinerLoaded()) {
            try {
                Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
                Field configField = liteminerClass.getField("CONFIG");
                Object config = configField.get(null);
                if (config != null) {
                    Field limitField = config.getClass().getField("blockBreakLimit");
                    Object limitObj = limitField.get(config);
                    Method getMethod = limitObj.getClass().getMethod("get");
                    return (int) getMethod.invoke(limitObj);
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.MAX_BLOCKS.get();
    }

    public static boolean shouldPreventToolBreaking() {
        if (isLiteMinerLoaded()) {
            try {
                Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
                Field configField = liteminerClass.getField("CONFIG");
                Object config = configField.get(null);
                if (config != null) {
                    Field field = config.getClass().getField("preventToolBreaking");
                    Object obj = field.get(config);
                    Method getMethod = obj.getClass().getMethod("get");
                    return (boolean) getMethod.invoke(obj);
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.PREVENT_TOOL_BREAKING.get();
    }

    public static float getFoodExhaustion() {
        if (isLiteMinerLoaded()) {
            try {
                Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
                Field configField = liteminerClass.getField("CONFIG");
                Object config = configField.get(null);
                if (config != null) {
                    Field enabledField = config.getClass().getField("foodExhaustionEnabled");
                    Object enabledObj = enabledField.get(config);
                    Method getEnabledMethod = enabledObj.getClass().getMethod("get");
                    if ((boolean) getEnabledMethod.invoke(enabledObj)) {
                        Field exhaustField = config.getClass().getField("foodExhaustion");
                        Object exhaustObj = exhaustField.get(config);
                        Method getExhaustMethod = exhaustObj.getClass().getMethod("get");
                        return ((Number) getExhaustMethod.invoke(exhaustObj)).floatValue();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.EXHAUSTION_PER_BLOCK.get().floatValue();
    }
}
