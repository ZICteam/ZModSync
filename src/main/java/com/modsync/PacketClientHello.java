package com.modsync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketClientHello {
    public static PacketClientHello decode(FriendlyByteBuf ignored) {
        return new PacketClientHello();
    }

    public static void encode(PacketClientHello ignored, FriendlyByteBuf buffer) {
    }

    public static void handle(PacketClientHello packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> NetworkHandler.handleClientHello(context));
        context.setPacketHandled(true);
    }
}
