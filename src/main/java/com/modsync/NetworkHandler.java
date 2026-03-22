package com.modsync;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final int CHUNK_SIZE = 12000;
    private static final int HANDSHAKE_TIMEOUT_TICKS = 100;
    private static final String REQUIRED_MOD_KICK_MESSAGE = "Missing required client sync mod. Install Forge and ModSync to join this server.";
    private static final Map<UUID, ChunkAccumulator> CLIENT_FILE_CHUNKS = new HashMap<>();
    private static final ChunkAccumulator SERVER_MANIFEST_CHUNKS = new ChunkAccumulator();
    private static final ChunkAccumulator START_DOWNLOAD_CHUNKS = new ChunkAccumulator();
    private static final Map<UUID, Integer> PENDING_HANDSHAKES = new ConcurrentHashMap<>();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ModSync.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private NetworkHandler() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++, PacketClientHello.class, PacketClientHello::encode, PacketClientHello::decode,
                PacketClientHello::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, PacketClientFileList.class, PacketClientFileList::encode, PacketClientFileList::decode,
                PacketClientFileList::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(packetId++, PacketServerManifest.class, PacketServerManifest::encode, PacketServerManifest::decode,
                PacketServerManifest::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(packetId++, PacketStartDownload.class, PacketStartDownload::encode, PacketStartDownload::decode,
                PacketStartDownload::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendClientHello() {
        CHANNEL.sendToServer(new PacketClientHello());
    }

    public static void sendClientFileList(List<ManifestEntry> entries) {
        sendChunkedToServer(ManifestGenerator.entriesToJson(entries));
    }

    static void handleClientHello(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            synchronized (CLIENT_FILE_CHUNKS) {
                CLIENT_FILE_CHUNKS.remove(player.getUUID());
            }
            PENDING_HANDSHAKES.remove(player.getUUID());
            LoggerUtils.info("Client connected for sync handshake: " + player.getGameProfile().getName());
        }
    }

    static void handleClientFileList(NetworkEvent.Context context, PacketClientFileList packet) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        String json;
        synchronized (CLIENT_FILE_CHUNKS) {
            ChunkAccumulator accumulator = CLIENT_FILE_CHUNKS.computeIfAbsent(player.getUUID(), ignored -> new ChunkAccumulator());
            json = accumulator.accept(packet.getChunkIndex(), packet.getTotalChunks(), packet.getPayload());
            if (json != null) {
                CLIENT_FILE_CHUNKS.remove(player.getUUID());
            }
        }
        if (json == null) {
            return;
        }

        List<ManifestEntry> clientEntries = ManifestGenerator.entriesFromJson(json);
        ManifestData manifest = ModSync.getLastManifest();
        if (manifest == null) {
            LoggerUtils.warn("Sync handshake requested before cached manifest was available");
            manifest = new ManifestData();
            manifest.setGeneratedAt(System.currentTimeMillis());
            manifest.setEntries(List.of());
        }

        String baseUrl = resolveBaseUrl(player);
        ManifestData outbound = attachDownloadUrls(manifest, baseUrl);
        List<ManifestEntry> required = SyncComparator.findMissingOrOutdated(clientEntries, outbound);

        LoggerUtils.info("File comparison for " + player.getGameProfile().getName() + " found " + required.size() + " downloads");

        sendChunkedToPlayer(player, ManifestGenerator.toJson(outbound), ChunkTarget.MANIFEST);
        sendChunkedToPlayer(player, ManifestGenerator.entriesToJson(required), ChunkTarget.START_DOWNLOAD);
    }

    static void handleServerManifest(PacketServerManifest packet) {
        String json = SERVER_MANIFEST_CHUNKS.accept(packet.getChunkIndex(), packet.getTotalChunks(), packet.getPayload());
        if (json == null) {
            return;
        }
        ManifestData manifestData = ManifestGenerator.fromJson(json);
        ModSync.setLastManifest(manifestData);
        LoggerUtils.info("Received server manifest with " + manifestData.getEntries().size() + " entries");
    }

    static void handleStartDownload(PacketStartDownload packet) {
        String json = START_DOWNLOAD_CHUNKS.accept(packet.getChunkIndex(), packet.getTotalChunks(), packet.getPayload());
        if (json == null) {
            return;
        }
        List<ManifestEntry> serverSuggested = ManifestGenerator.entriesFromJson(json);
        ManifestData manifestData = ModSync.getLastManifest();
        String serverId = ClientSyncContext.getCurrentServerId();
        List<ManifestEntry> localEntries = ServerSyncStatusCache.getCachedOrScanLocalEntries(serverId);
        if (manifestData != null) {
            SyncCleanupManager.cleanupObsoleteManagedFiles(serverId, manifestData, localEntries);
        }
        List<ManifestEntry> comparisonResult = manifestData == null
                ? serverSuggested
                : SyncComparator.findMissingOrOutdated(localEntries, manifestData);

        if (comparisonResult.isEmpty()) {
            LoggerUtils.info("Client is already synchronized");
            SyncCleanupManager.saveManagedManifest(serverId, manifestData);
            return;
        }

        LoggerUtils.info("Client starting download of " + comparisonResult.size() + " files");
        ensureProgressScreenVisible();
        DownloadManager.getInstance().startDownloads(comparisonResult,
                () -> SyncCleanupManager.saveManagedManifest(serverId, manifestData));
    }

    private static void ensureProgressScreenVisible() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> bootstrapClass = Class.forName("com.modsync.ClientBootstrap");
            bootstrapClass.getMethod("ensureProgressScreenVisible").invoke(null);
        } catch (Exception exception) {
            LoggerUtils.warn("Failed to open sync progress screen: " + exception.getMessage());
        }
    }

    public static void registerPendingHandshake(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PENDING_HANDSHAKES.put(player.getUUID(), HANDSHAKE_TIMEOUT_TICKS);
    }

    public static void clearPendingHandshake(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PENDING_HANDSHAKES.remove(player.getUUID());
        synchronized (CLIENT_FILE_CHUNKS) {
            CLIENT_FILE_CHUNKS.remove(player.getUUID());
        }
    }

    public static void tickPendingHandshakes(net.minecraft.server.MinecraftServer server) {
        if (PENDING_HANDSHAKES.isEmpty()) {
            return;
        }

        List<UUID> expired = new ArrayList<>();
        PENDING_HANDSHAKES.replaceAll((uuid, ticksLeft) -> {
            int next = ticksLeft - 1;
            if (next <= 0) {
                expired.add(uuid);
            }
            return next;
        });

        for (UUID uuid : expired) {
            PENDING_HANDSHAKES.remove(uuid);
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                LoggerUtils.warn("Disconnecting player without ModSync handshake: " + player.getGameProfile().getName());
                player.connection.disconnect(Component.literal(REQUIRED_MOD_KICK_MESSAGE));
            }
        }
    }

    private static ManifestData attachDownloadUrls(ManifestData manifestData, String baseUrl) {
        ManifestData copy = new ManifestData();
        copy.setGeneratedAt(manifestData.getGeneratedAt());
        List<ManifestEntry> entries = manifestData.getEntries().stream().map(entry -> {
            ManifestEntry cloned = entry.copy();
            cloned.setDownloadUrl(baseUrl + "/files/" + entry.getCategory().getHttpSegment() + "/" + entry.getRelativePath());
            return cloned;
        }).toList();
        copy.setEntries(entries);
        return copy;
    }

    private static String resolveBaseUrl(ServerPlayer player) {
        String bind = ConfigManager.serverHttpBind();
        String host = bind;

        if ("0.0.0.0".equals(bind) || "::".equals(bind)) {
            try {
                host = player.server.getLocalIp();
                if (host == null || host.isBlank()) {
                    InetAddress loopback = InetAddress.getLoopbackAddress();
                    host = loopback.getHostAddress();
                }
            } catch (Exception ignored) {
                host = "127.0.0.1";
            }
        }

        if (host.contains(":") && !host.startsWith("[")) {
            host = "[" + host + "]";
        }

        return "http://" + host + ":" + ConfigManager.serverHttpPort();
    }

    private static void sendChunkedToServer(String json) {
        List<String> chunks = splitJson(json);
        for (int i = 0; i < chunks.size(); i++) {
            CHANNEL.sendToServer(new PacketClientFileList(i, chunks.size(), chunks.get(i)));
        }
    }

    private static void sendChunkedToPlayer(ServerPlayer player, String json, ChunkTarget target) {
        List<String> chunks = splitJson(json);
        for (int i = 0; i < chunks.size(); i++) {
            switch (target) {
                case MANIFEST ->
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketServerManifest(i, chunks.size(), chunks.get(i)));
                case START_DOWNLOAD ->
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketStartDownload(i, chunks.size(), chunks.get(i)));
            }
        }
    }

    private static List<String> splitJson(String json) {
        List<String> chunks = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            chunks.add("");
            return chunks;
        }

        for (int index = 0; index < json.length(); index += CHUNK_SIZE) {
            chunks.add(json.substring(index, Math.min(json.length(), index + CHUNK_SIZE)));
        }
        return chunks;
    }

    private enum ChunkTarget {
        MANIFEST,
        START_DOWNLOAD
    }

    private static final class ChunkAccumulator {
        private int expectedChunks;
        private final StringBuilder builder = new StringBuilder();

        public String accept(int chunkIndex, int totalChunks, String payload) {
            if (chunkIndex == 0) {
                builder.setLength(0);
                expectedChunks = totalChunks;
            }

            builder.append(payload);
            if (chunkIndex + 1 < expectedChunks) {
                return null;
            }

            String result = builder.toString();
            builder.setLength(0);
            expectedChunks = 0;
            return result;
        }
    }
}
