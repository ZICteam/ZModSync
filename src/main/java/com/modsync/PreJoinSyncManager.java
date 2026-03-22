package com.modsync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PreJoinSyncManager {
    private PreJoinSyncManager() {
    }

    public static void startForServer(ServerData serverData, Screen returnScreen) {
        startForServer(serverData, returnScreen, true);
    }

    public static void startForServer(ServerData serverData, Screen returnScreen, boolean connectAfterSync) {
        Minecraft minecraft = Minecraft.getInstance();
        SyncLogBuffer.clear();
        SyncIssueState.clear();
        ClientSyncContext.setCurrentServerId(serverData.ip);
        LoggerUtils.info("Selected server: " + serverData.name + " (" + serverData.ip + ")");
        minecraft.setScreen(new SyncProgressScreen(returnScreen));

        CompletableFuture.runAsync(() -> {
            try {
                LoggerUtils.info("Requesting manifest over HTTP");
                int discoveredPort = MotdMetadataCodec.extractHttpPort(serverData);
                if (discoveredPort > 0) {
                    LoggerUtils.info("Discovered hidden ModSync HTTP port from MOTD: " + discoveredPort);
                }
                ManifestData manifest = ServerManifestHttpHandler.fetchManifest(serverData.ip, discoveredPort);
                ModSync.setLastManifest(manifest);
                LoggerUtils.info("Manifest received: " + manifest.getEntries().size() + " files");

                List<ManifestEntry> localEntries = ServerSyncStatusCache.getCachedOrScanLocalEntries(serverData.ip);
                SyncCleanupManager.cleanupObsoleteManagedFiles(serverData.ip, manifest, localEntries);
                List<ManifestEntry> required = SyncComparator.findMissingOrOutdated(localEntries, manifest);
                LoggerUtils.info("Pre-join sync found " + required.size() + " files to download");

                if (required.isEmpty()) {
                    LoggerUtils.info("Client is already synchronized");
                    SyncCleanupManager.saveManagedManifest(serverData.ip, manifest);
                    cacheCurrentState(serverData.ip);
                    ServerSyncStatusCache.requestRefresh(serverData);
                    if (connectAfterSync) {
                        continueConnecting(minecraft, returnScreen, serverData);
                    }
                    return;
                }

                DownloadManager.getInstance().startDownloads(required, () -> {
                    SyncCleanupManager.saveManagedManifest(serverData.ip, manifest);
                    cacheCurrentState(serverData.ip);
                    ServerSyncStatusCache.requestRefresh(serverData);
                    if (connectAfterSync) {
                        continueConnecting(minecraft, returnScreen, serverData);
                    }
                });
                ServerSyncStatusCache.markDirty(serverData);
            } catch (Exception exception) {
                SyncIssueState.set("Manifest step failed: " + exception.getMessage());
                LoggerUtils.error("Pre-join sync failed", exception);
                ServerSyncStatusCache.markDirty(serverData);
            }
        });
    }

    private static void continueConnecting(Minecraft minecraft, Screen returnScreen, ServerData serverData) {
        ClientSyncContext.markPreJoinReady(serverData.ip);
        minecraft.execute(() -> net.minecraft.client.gui.screens.ConnectScreen.startConnecting(
                returnScreen,
                minecraft,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(serverData.ip),
                serverData,
                false));
    }

    private static void cacheCurrentState(String serverId) {
        List<ManifestEntry> currentEntries = ClientFileScanner.scanLocalFiles(serverId);
        ServerSyncStatusCache.cacheLocalEntries(serverId, currentEntries);
    }
}
