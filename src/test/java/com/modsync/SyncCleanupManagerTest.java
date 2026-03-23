package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncCleanupManagerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        ManagedStateStore.resetForTests();
        RestartState.resetForTests();
    }

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

    @Test
    void cleanupObsoleteManagedFilesDeletesMatchingManagedFileAndMarksRestart() throws Exception {
        String serverId = "play.example.net:25565";
        Path stateRoot = tempDir.resolve("state");
        Path configRoot = tempDir.resolve("client/config");
        Files.createDirectories(configRoot.resolve("client"));
        Path targetFile = configRoot.resolve("client/options.txt");
        Files.writeString(targetFile, "server-managed");

        ManifestEntry previous = new ManifestEntry(
                CategoryType.CONFIG,
                "client/options.txt",
                "options.txt",
                Files.size(targetFile),
                HashUtils.sha256(targetFile),
                true,
                true,
                ""
        );
        ManagedStateStore.overrideStateRootForTests(stateRoot);
        ManagedStateStore.save(serverId, List.of(previous));

        ManifestData currentManifest = new ManifestData();
        currentManifest.setEntries(List.of());

        SyncCleanupManager.cleanupObsoleteManagedFiles(
                serverId,
                currentManifest,
                List.of(previous),
                category -> rootMap(configRoot).get(category),
                true
        );

        assertFalse(Files.exists(targetFile));
        assertFalse(Files.exists(configRoot.resolve("client")));
        assertTrue(RestartState.isRestartRequired());
        assertEquals(
                List.of("DELETED:CONFIG:client/options.txt"),
                RestartState.snapshotChanges().stream()
                        .map(change -> change.type().name() + ":" + change.category().name() + ":" + change.relativePath())
                        .toList()
        );
    }

    @Test
    void cleanupObsoleteManagedFilesSkipsLocallyModifiedFile() throws Exception {
        String serverId = "play.example.net:25565";
        Path stateRoot = tempDir.resolve("state");
        Path configRoot = tempDir.resolve("client/config");
        Files.createDirectories(configRoot.resolve("client"));
        Path targetFile = configRoot.resolve("client/options.txt");
        Files.writeString(targetFile, "old-managed");

        ManifestEntry previous = entry(CategoryType.CONFIG, "client/options.txt", "options.txt", Files.size(targetFile), HashUtils.sha256(targetFile));
        ManagedStateStore.overrideStateRootForTests(stateRoot);
        ManagedStateStore.save(serverId, List.of(previous));

        Files.writeString(targetFile, "locally-modified");
        ManifestEntry localModified = entry(CategoryType.CONFIG, "client/options.txt", "options.txt", Files.size(targetFile), HashUtils.sha256(targetFile));

        ManifestData currentManifest = new ManifestData();
        currentManifest.setEntries(List.of());

        SyncCleanupManager.cleanupObsoleteManagedFiles(
                serverId,
                currentManifest,
                List.of(localModified),
                category -> rootMap(configRoot).get(category),
                true
        );

        assertTrue(Files.exists(targetFile));
        assertFalse(RestartState.isRestartRequired());
        assertEquals(List.of(), RestartState.snapshotChanges());
    }

    private static Map<CategoryType, Path> rootMap(Path configRoot) {
        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.CONFIG, configRoot);
        return roots;
    }

    private static ManifestEntry entry(CategoryType category, String relativePath, String fileName, long size, String sha256) {
        return new ManifestEntry(category, relativePath, fileName, size, sha256, true, category.isDefaultRestartRequired(), "");
    }
}
