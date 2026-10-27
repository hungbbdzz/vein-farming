package com.velorise.veinfarming.network;

import com.velorise.veinfarming.FarmingEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Payload sent from client to server when the dedicated activation key is pressed or released.
 */
public record FarmingKeyPayload(boolean active) {

    public static void encode(FarmingKeyPayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active());
    }

    public static FarmingKeyPayload decode(FriendlyByteBuf buf) {
        return new FarmingKeyPayload(buf.readBoolean());
    }

    public static void handle(FarmingKeyPayload msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                FarmingEventHandler.setPlayerKeyActive(sp.getUUID(), msg.active());
            }
        });
        ctx.setPacketHandled(true);
    }
}
