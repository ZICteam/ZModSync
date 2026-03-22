package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncCleanupManagerTest {
    @Test
    void decideCleanupActionKeepsEntryThatStillExistsInManifest() {
        ManifestEntry previous = entry(CategoryType.MOD, "mods/a.jar", "a.jar", 10L, "aaa");

        SyncCleanupManager.CleanupAction action = SyncCleanupManager.decideCleanupAction(
                previous,
                Map.of(previous.getIdentityKey(), previous),
                Map.of()
        );

        assertEquals(SyncCleanupManager.CleanupAction.KEEP_PRESENT, action);
    }

    @Test
    void decideCleanupActionSkipsProtectedModsyncFile() {
        ManifestEntry previous = entry(CategoryType.MOD, "mods/modsync-1.0.15.jar", "modsync-1.0.15.jar", 10L, "aaa");

        SyncCleanupManager.CleanupAction action = SyncCleanupManager.decideCleanupAction(
                previous,
                Map.of(),
                Map.of(previous.getIdentityKey(), previous)
        );

        assertEquals(SyncCleanupManager.CleanupAction.SKIP_PROTECTED, action);
    }

    @Test
    void decideCleanupActionSkipsLocallyModifiedManagedFile() {
        ManifestEntry previous = entry(CategoryType.CONFIG, "client/options.txt", "options.txt", 10L, "aaa");
        ManifestEntry local = entry(CategoryType.CONFIG, "client/options.txt", "options.txt", 11L, "bbb");

        SyncCleanupManager.CleanupAction action = SyncCleanupManager.decideCleanupAction(
                previous,
                Map.of(),
                Map.of(previous.getIdentityKey(), local)
        );

        assertEquals(SyncCleanupManager.CleanupAction.SKIP_MODIFIED, action);
    }

    @Test
    void decideCleanupActionDeletesMatchingObsoleteFile() {
        ManifestEntry previous = entry(CategoryType.RESOURCEPACK, "packs/ui.zip", "ui.zip", 20L, "ccc");
        ManifestEntry local = entry(CategoryType.RESOURCEPACK, "packs/ui.zip", "ui.zip", 20L, "ccc");

        SyncCleanupManager.CleanupAction action = SyncCleanupManager.decideCleanupAction(
                previous,
                Map.of(),
                Map.of(previous.getIdentityKey(), local)
        );

        assertEquals(SyncCleanupManager.CleanupAction.DELETE, action);
    }

    private static ManifestEntry entry(CategoryType category, String relativePath, String fileName, long size, String sha256) {
        return new ManifestEntry(category, relativePath, fileName, size, sha256, true, category.isDefaultRestartRequired(), "");
    }
}
