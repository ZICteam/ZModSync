package com.modsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientFileScanner {
    private ClientFileScanner() {
    }

    public static List<ManifestEntry> scanLocalFiles() {
        return scanLocalFiles(ClientSyncContext.getCurrentServerId());
    }

    public static List<ManifestEntry> scanLocalFiles(String serverId) {
        Set<String> skipped = ConfigManager.skipFileExtensions();
        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);

        for (CategoryType category : CategoryType.values()) {
            if (!ConfigManager.isCategoryEnabled(category)) {
                continue;
            }
            roots.put(category, FileUtils.resolveClientTargetRoot(category, serverId));
        }

        List<ManifestEntry> entries = scanLocalFiles(serverId, roots, skipped);
        LoggerUtils.info("Client file scan found " + entries.size() + " files for " + (serverId == null || serverId.isBlank() ? "<default>" : serverId));
        return entries;
    }

    static List<ManifestEntry> scanLocalFiles(String serverId, Map<CategoryType, Path> roots, Set<String> skippedExtensions) {
        List<ManifestEntry> entries = new ArrayList<>();

        for (Map.Entry<CategoryType, Path> rootEntry : roots.entrySet()) {
            CategoryType category = rootEntry.getKey();
            Path root = rootEntry.getValue();
            if (!Files.exists(root)) {
                continue;
            }

            try (FileHashCache.ScopeSession hashCache = FileHashCache.openClientScope(serverId, category)) {
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .filter(file -> !FileUtils.isSkippedFile(file, skippedExtensions))
                        .forEach(file -> entries.add(toEntry(category, root, file, hashCache)));
            } catch (IOException exception) {
                LoggerUtils.error("Failed to scan client files for " + category, exception);
            }
        }
        return entries;
    }

    private static ManifestEntry toEntry(CategoryType category, Path root, Path file, FileHashCache.ScopeSession hashCache) {
        try {
            String relativePath = FileUtils.toRelativeUnixPath(root, file);
            FileHashCache.FileFingerprint fingerprint = hashCache.describe(file, relativePath);
            return new ManifestEntry(
                    category,
                    relativePath,
                    file.getFileName().toString(),
                    fingerprint.size(),
                    fingerprint.sha256(),
                    true,
                    category.isDefaultRestartRequired(),
                    ""
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to hash client file " + file, exception);
        }
    }
}
