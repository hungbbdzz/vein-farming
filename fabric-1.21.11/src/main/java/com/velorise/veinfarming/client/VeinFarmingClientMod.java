package com.velorise.veinfarming.client;

import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class VeinFarmingClientMod implements ClientModInitializer {
    private static boolean lastKeyActive = false;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.KEY_ACTIVATE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.KEY_TOGGLE_SMART_PLANT);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) {
                lastKeyActive = false;
                return;
            }

            // 1. Check toggle smart plant key
            while (ModKeyMappings.KEY_TOGGLE_SMART_PLANT.consumeClick()) {
                boolean newState = !ModKeyMappings.isSmartPlantEnabled();
                ModKeyMappings.setSmartPlantEnabled(newState);
                ClientPlayNetworking.send(new ToggleSmartPlantPayload(newState));

                Component msg = newState
                        ? Component.translatable("message.vein_farming.smart_plant.enabled").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("message.vein_farming.smart_plant.disabled").withStyle(ChatFormatting.RED);
                mc.player.displayClientMessage(msg, true);
            }

            // 2. Check activation key state changes to sync with server
            boolean currentActive = ModKeyMappings.isKeyActive();
            if (currentActive != lastKeyActive) {
                lastKeyActive = currentActive;
                ClientPlayNetworking.send(new FarmingKeyPayload(currentActive));
            }
        });

        // Register 3D Wireframe and Ghost highlight rendering
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(StandaloneHighlightRenderer::onBlockOutline);
        WorldRenderEvents.AFTER_ENTITIES.register(StandaloneHighlightRenderer::onRenderWorld);
    }
}

