package com.modsync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ManagedStateStore {
    private ManagedStateStore() {
    }

    public static List<ManifestEntry> load(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return List.of();
        }

        Path file = stateFile(serverId);
        if (!Files.exists(file)) {
            return List.of();
        }

        try {
            return ManifestGenerator.entriesFromJson(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            LoggerUtils.warn("Failed to load managed sync state for " + serverId + ": " + exception.getMessage());
            return List.of();
        }
    }

    public static void save(String serverId, List<ManifestEntry> entries) {
        if (serverId == null || serverId.isBlank()) {
            return;
        }

        Path file = stateFile(serverId);
        try {
            FileUtils.ensureParentExists(file);
            Files.writeString(file, ManifestGenerator.entriesToJson(entries), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LoggerUtils.error("Failed to save managed sync state for " + serverId, exception);
        }
    }

    private static Path stateFile(String serverId) {
        return FileUtils.configDir()
                .resolve("modsync-state")
                .resolve(FileUtils.sanitizeServerId(serverId) + "-" + Integer.toHexString(serverId.hashCode()) + ".json")
                .normalize();
    }
}
