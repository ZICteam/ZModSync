package com.modsync;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;

public final class ClientBootstrap {
    private static boolean initialized;

    private ClientBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        MinecraftForge.EVENT_BUS.register(ServerListDownloadHooks.class);
        initialized = true;
    }

    public static void ensureProgressScreenVisible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SyncProgressScreen || minecraft.screen instanceof RestartScreen) {
            return;
        }
        minecraft.setScreen(new SyncProgressScreen(minecraft.screen));
    }

    public static final class ClientEvents {
        @SubscribeEvent
        public void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.hasSingleplayerServer() || minecraft.getCurrentServer() == null) {
                LoggerUtils.info("Skipping ModSync handshake in singleplayer/local session");
                return;
            }

            LoggerUtils.info("Client connection established, starting sync handshake");
            ClientSyncContext.setCurrentServerId(minecraft.getCurrentServer().ip);
            NetworkHandler.sendClientHello();
            List<ManifestEntry> localFiles = ClientFileScanner.scanLocalFiles();
            ServerSyncStatusCache.cacheLocalEntries(ClientSyncContext.getCurrentServerId(), localFiles);
            NetworkHandler.sendClientFileList(localFiles);
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (!DownloadManager.getInstance().isActive() && RestartState.consumePromptPending()) {
                minecraft.setScreen(new RestartScreen());
            }
        }
    }
}
