package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartStateTest {
    @AfterEach
    void tearDown() {
        RestartState.resetForTests();
    }

    @Test
    void markRestartRequiredSetsFlagsAndPromptIsConsumableOnce() {
        RestartState.markRestartRequired();

        assertTrue(RestartState.isRestartRequired());
        assertTrue(RestartState.consumePromptPending());
        assertFalse(RestartState.consumePromptPending());
    }

    @Test
    void recordDownloadedAndDeletedPreserveInsertionOrder() {
        RestartState.recordDownloaded(entry(CategoryType.MOD, "mods/a.jar"));
        RestartState.recordDeleted(entry(CategoryType.CONFIG, "client/options.txt"));

        List<String> result = RestartState.snapshotChanges().stream()
                .map(change -> change.type().name() + ":" + change.category().name() + ":" + change.relativePath())
                .toList();

        assertEquals(List.of(
                "DOWNLOADED:MOD:mods/a.jar",
                "DELETED:CONFIG:client/options.txt"
        ), result);
    }

    @Test
    void recordingSameChangeTwiceDoesNotDuplicateEntry() {
        RestartState.recordDownloaded(entry(CategoryType.MOD, "mods/a.jar"));
        RestartState.recordDownloaded(entry(CategoryType.MOD, "mods/a.jar"));

        assertEquals(1, RestartState.snapshotChanges().size());
    }

    @Test
    void downloadedAndDeletedVariantsOfSameFileRemainSeparate() {
        ManifestEntry entry = entry(CategoryType.MOD, "mods/a.jar");

        RestartState.recordDownloaded(entry);
        RestartState.recordDeleted(entry);

        List<String> result = RestartState.snapshotChanges().stream()
                .map(change -> change.type().name() + ":" + change.relativePath())
                .toList();

        assertEquals(List.of(
                "DOWNLOADED:mods/a.jar",
                "DELETED:mods/a.jar"
        ), result);
    }

    private static ManifestEntry entry(CategoryType category, String relativePath) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        return new ManifestEntry(category, relativePath, fileName, 10L, "aaa", true, true, "");
    }
}
