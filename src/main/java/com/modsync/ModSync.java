package com.modsync;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(ModSync.MOD_ID)
public class ModSync {
    public static final String MOD_ID = "modsync";
    private static final long MANIFEST_REFRESH_INITIAL_DELAY_MINUTES = 5L;
    private static final long MANIFEST_REFRESH_PERIOD_MINUTES = 60L;

    private static volatile ManifestData lastManifest;
    private static ScheduledExecutorService manifestRefreshExecutor;
    private static String visibleServerMotd = "";

    public ModSync() {
        ConfigManager.register();
        LoggerUtils.init(ConfigManager.logFile(), ConfigManager.logToFile());
        LoggerUtils.info("ModSync startup");
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
        bootstrapClientHooks();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static ManifestData getLastManifest() {
        return lastManifest;
    }

    public static void setLastManifest(ManifestData manifestData) {
        lastManifest = manifestData;
    }

    public static long getManifestRefreshInitialDelayMinutes() {
        return MANIFEST_REFRESH_INITIAL_DELAY_MINUTES;
    }

    public static long getManifestRefreshPeriodMinutes() {
        return MANIFEST_REFRESH_PERIOD_MINUTES;
    }

    private static synchronized void refreshManifest(String reason) {
        try {
            ManifestData manifest = ManifestGenerator.generateManifest();
            setLastManifest(manifest);
            HttpFileServer.getInstance().updateManifest(manifest);
            LoggerUtils.info("Manifest cache refreshed: " + reason);
        } catch (Exception exception) {
            LoggerUtils.error("Failed to refresh manifest cache: " + reason, exception);
        }
    }

    private static synchronized void startManifestRefreshTask() {
        stopManifestRefreshTask();
        manifestRefreshExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "modsync-manifest-refresh");
            thread.setDaemon(true);
            return thread;
        });
        manifestRefreshExecutor.scheduleAtFixedRate(
                () -> refreshManifest("scheduled"),
                MANIFEST_REFRESH_INITIAL_DELAY_MINUTES,
                MANIFEST_REFRESH_PERIOD_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private static synchronized void stopManifestRefreshTask() {
        if (manifestRefreshExecutor != null) {
            manifestRefreshExecutor.shutdownNow();
            manifestRefreshExecutor = null;
        }
    }

    public static synchronized void reloadServerRuntime(String reason) {
        try {
            ConfigManager.reloadFromDisk();
        } catch (Exception exception) {
            LoggerUtils.error("Failed to reload ModSync config from disk", exception);
            return;
        }

        HttpFileServer.getInstance().stop();
        updateServerMotdMetadata();
        refreshManifest(reason);
        HttpFileServer.getInstance().start();
        startManifestRefreshTask();
        LoggerUtils.info("ModSync runtime reloaded: " + reason);
    }

    private static synchronized void updateServerMotdMetadata() {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        if (visibleServerMotd.isBlank()) {
            visibleServerMotd = MotdMetadataCodec.stripHiddenMetadata(server.getMotd());
        }

        server.setMotd(MotdMetadataCodec.appendHiddenHttpPort(visibleServerMotd, ConfigManager.serverHttpPort()));
        LoggerUtils.info("Embedded hidden ModSync HTTP port metadata into server MOTD");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            FileUtils.ensureServerRepositoryDirectories();
            LoggerUtils.info("Ensured ModSync server repository directories");
        } catch (Exception exception) {
            LoggerUtils.error("Failed to create ModSync server repository directories", exception);
        }

        visibleServerMotd = MotdMetadataCodec.stripHiddenMetadata(event.getServer().getMotd());
        updateServerMotdMetadata();
        refreshManifest("server start");
        HttpFileServer.getInstance().start();
        startManifestRefreshTask();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        stopManifestRefreshTask();
        HttpFileServer.getInstance().stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModSyncCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.registerPendingHandshake(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkHandler.clearPendingHandshake(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            NetworkHandler.tickPendingHandshakes(event.getServer());
        }
    }

    @SubscribeEvent
    public void onConfigReload(ModConfigEvent.Reloading event) {
        LoggerUtils.info("Configuration reloaded");
    }

    private static void bootstrapClientHooks() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            Class<?> bootstrapClass = Class.forName("com.modsync.ClientBootstrap");
            bootstrapClass.getMethod("init").invoke(null);
        } catch (Exception exception) {
            LoggerUtils.error("Failed to bootstrap client hooks", exception);
        }
    }
}
