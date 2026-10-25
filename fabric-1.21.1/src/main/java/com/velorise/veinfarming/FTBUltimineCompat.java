package com.velorise.veinfarming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FTBUltimineCompat {
    public static final String FTB_ULTIMINE_MODID = "ftbultimine";

    private static Boolean modLoaded = null;
    private static boolean serverReflInit = false;
    private static Method getInstanceMethod;
    private static Method getPlayerDataMethod;
    private static Method isPressedMethod;
    private static Method hasCachedMethod;
    private static Method cachedPositionsMethod;
    private static Method updateBlocksMethod;
    private static Method addPendingXPMethod;
    private static Method takePendingXPMethod;
    private static Method getMaxBlocksMethod;
    private static Field preventToolBreakField;
    private static Method preventToolBreakGetMethod;
    private static Method getExhaustionMethod;
    private static Method setCooldownMethod;

    private static boolean clientReflInit = false;
    private static Field clientKeyBindField;

    public static boolean isFTBUltimineLoaded() {
        if (modLoaded == null) {
            modLoaded = FabricLoader.getInstance().isModLoaded(FTB_ULTIMINE_MODID);
        }
        return modLoaded;
    }

    private static synchronized void initServerReflection() {
        if (serverReflInit) return;
        serverReflInit = true;
        try {
            Class<?> ftbClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
            getInstanceMethod = ftbClass.getMethod("getInstance");
            getPlayerDataMethod = ftbClass.getMethod("getOrCreatePlayerData", Player.class);

            Class<?> playerDataClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltiminePlayerData");
            isPressedMethod = playerDataClass.getMethod("isPressed");
            hasCachedMethod = playerDataClass.getMethod("hasCachedPositions");
            cachedPositionsMethod = playerDataClass.getMethod("cachedPositions");
            updateBlocksMethod = playerDataClass.getMethod("updateBlocks", ServerPlayer.class, BlockPos.class, Direction.class, boolean.class, int.class);
            addPendingXPMethod = playerDataClass.getMethod("addPendingXPCost", ServerPlayer.class, int.class);
            takePendingXPMethod = playerDataClass.getMethod("takePendingXP", ServerPlayer.class);

            Class<?> configClass = Class.forName("dev.ftb.mods.ftbultimine.config.FTBUltimineServerConfig");
            getMaxBlocksMethod = configClass.getMethod("getMaxBlocks", ServerPlayer.class);
            getExhaustionMethod = configClass.getMethod("getExhaustionPerBlock", ServerPlayer.class);
            preventToolBreakField = configClass.getField("PREVENT_TOOL_BREAK");
            Object intVal = preventToolBreakField.get(null);
            if (intVal != null) {
                preventToolBreakGetMethod = intVal.getClass().getMethod("get");
            }

            Class<?> trackerClass = Class.forName("dev.ftb.mods.ftbultimine.CooldownTracker");
            setCooldownMethod = trackerClass.getMethod("setLastUltimineTime", Player.class, long.class);
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("FTB Ultimine reflection initialization note", t);
        }
    }

    private static synchronized void initClientReflection() {
        if (clientReflInit) return;
        clientReflInit = true;
        try {
            Class<?> clientClass = Class.forName("dev.ftb.mods.ftbultimine.client.FTBUltimineClient");
            clientKeyBindField = clientClass.getField("keyBindUltimine");
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("FTB Ultimine client reflection initialization note", t);
        }
    }

    /**
     * Checks if the player is currently holding the FTB Ultimine activation key.
     * Uses cached reflection for clean soft-dependency without per-frame lookup overhead.
     */
    public static boolean isUltimineActive(Player player) {
        if (!isFTBUltimineLoaded() || player == null) {
            return false;
        }
        initServerReflection();
        try {
            if (getInstanceMethod != null && getPlayerDataMethod != null && isPressedMethod != null) {
                Object instance = getInstanceMethod.invoke(null);
                if (instance != null) {
                    Object data = getPlayerDataMethod.invoke(instance, player);
                    if (data != null) {
                        return (boolean) isPressedMethod.invoke(data);
                    }
                }
            }
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("FTB Ultimine reflection check failed", t);
        }
        return false;
    }

    /**
     * Checks if the client player is pressing the FTB Ultimine keybind.
     */
    public static boolean isUltimineClientActive() {
        if (!isFTBUltimineLoaded()) {
            return false;
        }
        initClientReflection();
        try {
            if (clientKeyBindField != null) {
                net.minecraft.client.KeyMapping keyMapping = (net.minecraft.client.KeyMapping) clientKeyBindField.get(null);
                if (keyMapping != null) {
                    return keyMapping.isDown();
                }
            }
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("FTB Ultimine client reflection check failed", t);
        }
        return false;
    }

    public static Collection<BlockPos> getClientSelectedBlocks(Player player, BlockPos targetPos) {
        if (!isFTBUltimineLoaded() || player == null || targetPos == null) {
            return Collections.emptyList();
        }
        initServerReflection();
        try {
            if (getInstanceMethod != null && getPlayerDataMethod != null && hasCachedMethod != null && cachedPositionsMethod != null) {
                Object instance = getInstanceMethod.invoke(null);
                if (instance != null) {
                    Object data = getPlayerDataMethod.invoke(instance, player);
                    if (data != null && (boolean) hasCachedMethod.invoke(data)) {
                        Object coll = cachedPositionsMethod.invoke(data);
                        if (coll instanceof Collection<?> collection && !collection.isEmpty()) {
                            List<BlockPos> list = new ArrayList<>(collection.size());
                            for (Object obj : collection) {
                                if (obj instanceof BlockPos bp) {
                                    list.add(bp);
                                }
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

    public static Collection<BlockPos> getSelectedBlocks(ServerPlayer player, BlockPos targetPos) {
        return getSelectedBlocks(player, targetPos, Direction.UP);
    }

    public static Collection<BlockPos> getSelectedBlocks(ServerPlayer player, BlockPos targetPos, Direction face) {
        if (!isFTBUltimineLoaded() || player == null || targetPos == null) {
            return Collections.emptyList();
        }
        initServerReflection();
        try {
            if (getInstanceMethod == null || getPlayerDataMethod == null || isPressedMethod == null) {
                return Collections.emptyList();
            }
            Object instance = getInstanceMethod.invoke(null);
            if (instance == null) {
                return Collections.emptyList();
            }
            Object data = getPlayerDataMethod.invoke(instance, player);
            if (data == null || !((boolean) isPressedMethod.invoke(data))) {
                return Collections.emptyList();
            }

            int maxBlocks = getEffectiveBlockLimit(player);

            if (hasCachedMethod != null && cachedPositionsMethod != null && (boolean) hasCachedMethod.invoke(data)) {
                Object coll = cachedPositionsMethod.invoke(data);
                if (coll instanceof Collection<?> collection && !collection.isEmpty()) {
                    boolean containsTarget = false;
                    for (Object obj : collection) {
                        if (obj instanceof BlockPos bp) {
                            if (bp.equals(targetPos) || bp.equals(targetPos.above()) || bp.equals(targetPos.below())) {
                                containsTarget = true;
                                break;
                            }
                        }
                    }
                    if (containsTarget) {
                        List<BlockPos> result = new ArrayList<>(collection.size());
                        for (Object obj : collection) {
                            if (obj instanceof BlockPos bp) {
                                result.add(bp);
                            }
                        }
                        return result;
                    }
                }
            }

            if (updateBlocksMethod != null && cachedPositionsMethod != null) {
                Direction useFace = face != null ? face : Direction.UP;
                updateBlocksMethod.invoke(data, player, targetPos, useFace, false, maxBlocks);

                Object coll = cachedPositionsMethod.invoke(data);
                if (coll instanceof Collection<?> collection && !collection.isEmpty()) {
                    List<BlockPos> result = new ArrayList<>(collection.size());
                    for (Object obj : collection) {
                        if (obj instanceof BlockPos bp) {
                            result.add(bp);
                        }
                    }
                    return result;
                }
            }
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("FTB Ultimine getSelectedBlocks reflection failed", t);
        }
        return Collections.emptyList();
    }

    public static int getEffectiveBlockLimit(ServerPlayer player) {
        if (isFTBUltimineLoaded() && player != null) {
            initServerReflection();
            try {
                if (getMaxBlocksMethod != null) {
                    return (int) getMaxBlocksMethod.invoke(null, player);
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.MAX_BLOCKS.get();
    }

    public static boolean shouldPreventToolBreaking() {
        if (isFTBUltimineLoaded()) {
            initServerReflection();
            try {
                if (preventToolBreakField != null && preventToolBreakGetMethod != null) {
                    Object intVal = preventToolBreakField.get(null);
                    if (intVal != null) {
                        int threshold = (int) preventToolBreakGetMethod.invoke(intVal);
                        return threshold > 0;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.PREVENT_TOOL_BREAKING.get();
    }

    public static float getFoodExhaustion(ServerPlayer player) {
        if (isFTBUltimineLoaded() && player != null) {
            initServerReflection();
            try {
                if (getExhaustionMethod != null) {
                    double exhaustion = (double) getExhaustionMethod.invoke(null, player);
                    return (float) exhaustion;
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.EXHAUSTION_PER_BLOCK.get().floatValue();
    }

    public static void applyPostUltimineCosts(ServerPlayer player, int count) {
        if (!isFTBUltimineLoaded() || player == null || count <= 0) {
            return;
        }
        initServerReflection();
        try {
            if (setCooldownMethod != null) {
                setCooldownMethod.invoke(null, player, System.currentTimeMillis());
            }
            if (!player.isCreative() && getInstanceMethod != null && getPlayerDataMethod != null
                    && addPendingXPMethod != null && takePendingXPMethod != null) {
                Object instance = getInstanceMethod.invoke(null);
                if (instance != null) {
                    Object data = getPlayerDataMethod.invoke(instance, player);
                    if (data != null) {
                        int extraBlocks = Math.max(0, count - 1);
                        addPendingXPMethod.invoke(data, player, extraBlocks);
                        takePendingXPMethod.invoke(data, player);
                    }
                }
            }
        } catch (Throwable t) {
            VeinFarmingMod.LOGGER.debug("Failed to apply post FTB Ultimine costs", t);
        }
    }
}
