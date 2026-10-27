package com.velorise.veinfarming.network;

import com.velorise.veinfarming.FarmingEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Payload sent from client to server when smart planting mode is toggled on/off.
 */
public record ToggleSmartPlantPayload(boolean enabled) {

    public static void encode(ToggleSmartPlantPayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled());
    }

    public static ToggleSmartPlantPayload decode(FriendlyByteBuf buf) {
        return new ToggleSmartPlantPayload(buf.readBoolean());
    }

    public static void handle(ToggleSmartPlantPayload msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                FarmingEventHandler.setPlayerSmartPlantEnabled(sp.getUUID(), msg.enabled());
            }
        });
        ctx.setPacketHandled(true);
    }
}
