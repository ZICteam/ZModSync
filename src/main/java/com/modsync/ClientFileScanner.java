package com.modsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ClientFileScanner {
    private ClientFileScanner() {
    }

    public static List<ManifestEntry> scanLocalFiles() {
        return scanLocalFiles(ClientSyncContext.getCurrentServerId());
    }

    public static List<ManifestEntry> scanLocalFiles(String serverId) {
        List<ManifestEntry> entries = new ArrayList<>();
        Set<String> skipped = ConfigManager.skipFileExtensions();

        for (CategoryType category : CategoryType.values()) {
            if (!ConfigManager.isCategoryEnabled(category)) {
                continue;
            }

            Path root = FileUtils.resolveClientTargetRoot(category, serverId);
            if (!Files.exists(root)) {
                continue;
            }

            try (FileHashCache.ScopeSession hashCache = FileHashCache.openClientScope(serverId, category)) {
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .filter(file -> !FileUtils.isSkippedFile(file, skipped))
                        .forEach(file -> entries.add(toEntry(category, root, file, hashCache)));
            } catch (IOException exception) {
                LoggerUtils.error("Failed to scan client files for " + category, exception);
            }
        }

        LoggerUtils.info("Client file scan found " + entries.size() + " files for " + (serverId == null || serverId.isBlank() ? "<default>" : serverId));
        return entries;
    }

    private static ManifestEntry toEntry(CategoryType category, Path root, Path file, FileHashCache.ScopeSession hashCache) {
        try {
            String relativePath = FileUtils.toRelativeUnixPath(root, file);
            return new ManifestEntry(
                    category,
                    relativePath,
                    file.getFileName().toString(),
                    Files.size(file),
                    hashCache.sha256(file, relativePath),
                    true,
                    category.isDefaultRestartRequired(),
                    ""
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to hash client file " + file, exception);
        }
    }
}
