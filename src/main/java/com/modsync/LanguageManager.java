package com.modsync;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.HashMap;
import java.util.Map;

public final class LanguageManager {
    private static final Map<String, String> FALLBACK_EN = new HashMap<>();

    static {
        FALLBACK_EN.put("modsync.checking", "Checking server synchronization manifest...");
        FALLBACK_EN.put("modsync.downloading", "Downloading synchronized files...");
        FALLBACK_EN.put("modsync.download_complete", "Downloads complete.");
        FALLBACK_EN.put("modsync.restart_required", "New mods or resources were downloaded. Restart Minecraft to apply them.");
        FALLBACK_EN.put("modsync.restart_now", "Restart Now");
        FALLBACK_EN.put("modsync.restart_later", "Later");
        FALLBACK_EN.put("modsync.restart.updated_header", "Updated files");
        FALLBACK_EN.put("modsync.restart.change.downloaded", "Updated");
        FALLBACK_EN.put("modsync.restart.change.deleted", "Removed");
        FALLBACK_EN.put("modsync.restart.more", "... and %1$s more");
        FALLBACK_EN.put("modsync.download_button", "Download Mods");
        FALLBACK_EN.put("modsync.download_short", "Download");
        FALLBACK_EN.put("modsync.refresh", "Refresh");
        FALLBACK_EN.put("modsync.back", "Back");
        FALLBACK_EN.put("modsync.logs", "Logs");
        FALLBACK_EN.put("modsync.progress.title", "SyncBridge Progress");
        FALLBACK_EN.put("modsync.progress.none", "No downloads are currently active.");
        FALLBACK_EN.put("modsync.progress.state_waiting", "Checking");
        FALLBACK_EN.put("modsync.progress.state_downloading", "Downloading");
        FALLBACK_EN.put("modsync.progress.state_complete", "Synchronized");
        FALLBACK_EN.put("modsync.progress.state_failed", "Sync Failed");
        FALLBACK_EN.put("modsync.progress.state_restart", "Restart Required");
        FALLBACK_EN.put("modsync.error.title", "Sync Error");
        FALLBACK_EN.put("modsync.error.check_logs", "Open the log panel below for details.");
        FALLBACK_EN.put("modsync.error.update_required", "Your mods are outdated. Update them with the download button before connecting.");
        FALLBACK_EN.put("modsync.multiplayer.empty", "No saved servers yet.");
        FALLBACK_EN.put("modsync.multiplayer.details", "Server Details");
        FALLBACK_EN.put("modsync.multiplayer.no_selection", "Select a server from the list.");
        FALLBACK_EN.put("modsync.multiplayer.ping", "Ping");
        FALLBACK_EN.put("modsync.multiplayer.version", "Version");
        FALLBACK_EN.put("modsync.multiplayer.sync", "Sync");
        FALLBACK_EN.put("modsync.status.unknown", "Not checked");
        FALLBACK_EN.put("modsync.status.synced", "Synchronized");
        FALLBACK_EN.put("modsync.status.outdated", "Update needed");
        FALLBACK_EN.put("modsync.status.error", "Manifest unavailable");
        FALLBACK_EN.put("modsync.status.checking", "Checking");
        FALLBACK_EN.put("modsync.status.restart_pending", "Restart Minecraft to activate this server's synced mods.");
        FALLBACK_EN.put("modsync.server_name_default", "New Server");
        FALLBACK_EN.put("modsync.direct_server_default", "Direct Server");
        FALLBACK_EN.put("modsync.restart.title", "Restart Required");
    }

    private LanguageManager() {
    }

    public static String get(String key) {
        if (FMLLoader.getDist().isClient() && I18n.exists(key)) {
            return I18n.get(key);
        }
        return FALLBACK_EN.getOrDefault(key, key);
    }

    public static Component component(String key, Object... args) {
        if (FMLLoader.getDist().isClient()) {
            return Component.translatable(key, args);
        }
        return Component.literal(args.length == 0 ? get(key) : String.format(get(key), args));
    }
}
