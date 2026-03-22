package com.modsync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketStartDownload {
    private final int chunkIndex;
    private final int totalChunks;
    private final String payload;

    public PacketStartDownload(int chunkIndex, int totalChunks, String payload) {
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

    public static PacketStartDownload decode(FriendlyByteBuf buffer) {
        return new PacketStartDownload(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(16384));
    }

    public static void encode(PacketStartDownload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.chunkIndex);
        buffer.writeVarInt(packet.totalChunks);
        buffer.writeUtf(packet.payload);
    }

    public static void handle(PacketStartDownload packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> NetworkHandler.handleStartDownload(packet));
        context.setPacketHandled(true);
    }
}
