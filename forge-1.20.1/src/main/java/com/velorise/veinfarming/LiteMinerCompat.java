package com.velorise.veinfarming;

import net.minecraftforge.fml.ModList;
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

    private static Boolean modLoaded = null;
    private static boolean serverReflInit = false;
    private static Field instanceField;
    private static Method getPlayerStateMethod;
    private static Method getKeymappingStateMethod;
    private static Method getShapeMethod;
    private static Field walkersField;
    private static Field configField;
    private static Field blockBreakLimitField;
    private static Method blockBreakLimitGetMethod;
    private static Field preventToolBreakingField;
    private static Method preventToolBreakingGetMethod;
    private static Field foodExhaustionEnabledField;
    private static Method foodExhaustionEnabledGetMethod;
    private static Field foodExhaustionField;
    private static Method foodExhaustionGetMethod;

    private static boolean clientReflInit = false;
    private static Method isVeinMiningMethod;
    private static Field clientKeyField;
    private static Field selectedBlocksField;

    public static boolean isLiteMinerLoaded() {
        if (modLoaded == null) {
            modLoaded = ModList.get().isLoaded(LITEMINER_MODID);
        }
        return modLoaded;
    }

    private static synchronized void initServerReflection() {
        if (serverReflInit) return;
        serverReflInit = true;
        try {
            Class<?> liteminerClass = Class.forName("com.iamkaf.liteminer.Liteminer");
            instanceField = liteminerClass.getField("instance");
            walkersField = liteminerClass.getField("WALKERS");
            configField = liteminerClass.getField("CONFIG");

            Object instance = instanceField.get(null);
            if (instance != null) {
                getPlayerStateMethod = instance.getClass().getMethod("getPlayerState", ServerPlayer.class);
            }

            Object config = configField.get(null);
            if (config != null) {
                blockBreakLimitField = config.getClass().getField("blockBreakLimit");
                Object limitObj = blockBreakLimitField.get(config);
                if (limitObj != null) blockBreakLimitGetMethod = limitObj.getClass().getMethod("get");

                preventToolBreakingField = config.getClass().getField("preventToolBreaking");
                Object ptbObj = preventToolBreakingField.get(config);
                if (ptbObj != null) preventToolBreakingGetMethod = ptbObj.getClass().getMethod("get");

                foodExhaustionEnabledField = config.getClass().getField("foodExhaustionEnabled");
                Object feeObj = foodExhaustionEnabledField.get(config);
                if (feeObj != null) foodExhaustionEnabledGetMethod = feeObj.getClass().getMethod("get");

                foodExhaustionField = config.getClass().getField("foodExhaustion");
                Object feObj = foodExhaustionField.get(config);
                if (feObj != null) foodExhaustionGetMethod = feObj.getClass().getMethod("get");
            }
        } catch (Throwable ignored) {
        }
    }

    private static synchronized void initClientReflection() {
        if (clientReflInit) return;
        clientReflInit = true;
        try {
            Class<?> clientClass = Class.forName("com.iamkaf.liteminer.LiteminerClient");
            isVeinMiningMethod = clientClass.getMethod("isVeinMining");
            clientKeyField = clientClass.getField("KEY_MAPPING");
            selectedBlocksField = clientClass.getField("selectedBlocks");
        } catch (Throwable ignored) {
        }
    }

    public static boolean isLiteMinerActive(ServerPlayer player) {
        if (!isLiteMinerLoaded() || player == null) {
            return false;
        }
        initServerReflection();
        try {
            if (instanceField != null) {
                Object instance = instanceField.get(null);
                if (instance != null) {
                    if (getPlayerStateMethod == null) {
                        getPlayerStateMethod = instance.getClass().getMethod("getPlayerState", ServerPlayer.class);
                    }
                    Object state = getPlayerStateMethod.invoke(instance, player);
                    if (state != null) {
                        if (getKeymappingStateMethod == null) {
                            getKeymappingStateMethod = state.getClass().getMethod("getKeymappingState");
                        }
                        return (boolean) getKeymappingStateMethod.invoke(state);
                    }
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
        initClientReflection();
        try {
            if (isVeinMiningMethod != null && (boolean) isVeinMiningMethod.invoke(null)) {
                return true;
            }
            if (clientKeyField != null) {
                net.minecraft.client.KeyMapping keyMapping = (net.minecraft.client.KeyMapping) clientKeyField.get(null);
                return keyMapping != null && keyMapping.isDown();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Collection<BlockPos> getClientSelectedBlocks(Level level, Player player, BlockPos targetPos) {
        if (!isLiteMinerLoaded()) {
            return Collections.emptyList();
        }
        initClientReflection();
        try {
            if (selectedBlocksField != null) {
                Collection<?> set = (Collection<?>) selectedBlocksField.get(null);
                if (set != null && !set.isEmpty()) {
                    List<BlockPos> list = new ArrayList<>(set.size());
                    for (Object o : set) {
                        if (o instanceof BlockPos bp) list.add(bp);
                    }
                    return list;
                }
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static Collection<BlockPos> getSelectedBlocks(ServerPlayer player, BlockPos targetPos) {
        if (!isLiteMinerLoaded() || player == null || targetPos == null) {
            return Collections.emptyList();
        }
        initServerReflection();
        try {
            if (instanceField != null && walkersField != null) {
                Object instance = instanceField.get(null);
                if (instance != null) {
                    if (getPlayerStateMethod == null) {
                        getPlayerStateMethod = instance.getClass().getMethod("getPlayerState", ServerPlayer.class);
                    }
                    Object state = getPlayerStateMethod.invoke(instance, player);
                    if (state != null) {
                        if (getShapeMethod == null) {
                            getShapeMethod = state.getClass().getMethod("getShape");
                        }
                        int shapeIndex = (int) getShapeMethod.invoke(state);
                        List<?> walkers = (List<?>) walkersField.get(null);
                        if (walkers != null && shapeIndex >= 0 && shapeIndex < walkers.size()) {
                            Object walker = walkers.get(shapeIndex);
                            Method walkMethod = walker.getClass().getMethod("walk", Level.class, Player.class, BlockPos.class);
                            Object walked = walkMethod.invoke(walker, player.level(), player, targetPos);
                            if (walked instanceof Collection<?> col) {
                                List<BlockPos> list = new ArrayList<>(col.size());
                                for (Object o : col) {
                                    if (o instanceof BlockPos bp) list.add(bp);
                                }
                                return list;
                            }
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
            initServerReflection();
            try {
                if (configField != null && blockBreakLimitField != null && blockBreakLimitGetMethod != null) {
                    Object config = configField.get(null);
                    if (config != null) {
                        Object limitObj = blockBreakLimitField.get(config);
                        return (int) blockBreakLimitGetMethod.invoke(limitObj);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.MAX_BLOCKS.get();
    }

    public static boolean shouldPreventToolBreaking() {
        if (isLiteMinerLoaded()) {
            initServerReflection();
            try {
                if (configField != null && preventToolBreakingField != null && preventToolBreakingGetMethod != null) {
                    Object config = configField.get(null);
                    if (config != null) {
                        Object obj = preventToolBreakingField.get(config);
                        return (boolean) preventToolBreakingGetMethod.invoke(obj);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.PREVENT_TOOL_BREAKING.get();
    }

    public static float getFoodExhaustion() {
        if (isLiteMinerLoaded()) {
            initServerReflection();
            try {
                if (configField != null && foodExhaustionEnabledField != null && foodExhaustionEnabledGetMethod != null
                        && foodExhaustionField != null && foodExhaustionGetMethod != null) {
                    Object config = configField.get(null);
                    if (config != null) {
                        Object enabledObj = foodExhaustionEnabledField.get(config);
                        if ((boolean) foodExhaustionEnabledGetMethod.invoke(enabledObj)) {
                            Object exhaustObj = foodExhaustionField.get(config);
                            return ((Number) foodExhaustionGetMethod.invoke(exhaustObj)).floatValue();
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return FarmingConfig.EXHAUSTION_PER_BLOCK.get().floatValue();
    }
}
