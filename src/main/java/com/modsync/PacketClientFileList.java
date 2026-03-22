package com.modsync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketClientFileList {
    private final int chunkIndex;
    private final int totalChunks;
    private final String payload;

    public PacketClientFileList(int chunkIndex, int totalChunks, String payload) {
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.payload = payload;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public String getPayload() {
        return payload;
    }

    public static PacketClientFileList decode(FriendlyByteBuf buffer) {
        return new PacketClientFileList(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(16384));
    }

    public static void encode(PacketClientFileList packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeVarInt(packet.totalChunks);
        buffer.writeUtf(packet.payload);
    }

    public static void handle(PacketClientFileList packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> NetworkHandler.handleClientFileList(context, packet));
        context.setPacketHandled(true);
    }
}
