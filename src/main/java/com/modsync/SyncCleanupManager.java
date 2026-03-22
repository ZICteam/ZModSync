package com.modsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SyncCleanupManager {
    private SyncCleanupManager() {
    }

    public static void cleanupObsoleteManagedFiles(String serverId, ManifestData manifestData) {
        cleanupObsoleteManagedFiles(serverId, manifestData, ClientFileScanner.scanLocalFiles());
    }

    public static void cleanupObsoleteManagedFiles(String serverId, ManifestData manifestData, List<ManifestEntry> localEntries) {
        if (!ConfigManager.deleteInvalidFiles() || serverId == null || serverId.isBlank() || manifestData == null) {
            return;
        }

        Map<String, ManifestEntry> currentEntries = new HashMap<>();
        for (ManifestEntry entry : manifestData.getEntries()) {
            currentEntries.put(entry.getIdentityKey(), entry);
        }

        List<ManifestEntry> previousEntries = ManagedStateStore.load(serverId);
        int deletedCount = 0;

        for (ManifestEntry previousEntry : previousEntries) {
            if (currentEntries.containsKey(previousEntry.getIdentityKey())) {
                continue;
            }

            Path root = FileUtils.resolveClientTargetRoot(previousEntry.getCategory(), serverId);
            Path file = FileUtils.resolveSafeChild(root, previousEntry.getRelativePath());
            try {
                if (Files.deleteIfExists(file)) {
                    deletedCount++;
                    LoggerUtils.info("Deleted obsolete managed file: " + previousEntry.getCategory().name()
                            + " / " + previousEntry.getRelativePath());
                    deleteEmptyParents(root, file.getParent());
                    if (previousEntry.isRestartRequired()) {
                        RestartState.recordDeleted(previousEntry);
                        RestartState.markRestartRequired();
                    }
                }
            } catch (Exception exception) {
                LoggerUtils.warn("Failed to delete obsolete managed file "
                        + previousEntry.getRelativePath() + ": " + exception.getMessage());
            }
        }

        if (deletedCount > 0) {
            LoggerUtils.info("Removed " + deletedCount + " obsolete managed files for " + serverId);
        }
    }

    public static void saveManagedManifest(String serverId, ManifestData manifestData) {
        if (manifestData == null) {
            return;
        }
        ManagedStateStore.save(serverId, manifestData.getEntries());
    }

    private static void deleteEmptyParents(Path root, Path start) throws IOException {
        Path normalizedRoot = root.normalize();
        Path current = start;
        while (current != null && !current.equals(normalizedRoot) && current.startsWith(normalizedRoot)) {
            try {
                Files.delete(current);
            } catch (IOException exception) {
                break;
            }
            current = current.getParent();
        }
    }
}
