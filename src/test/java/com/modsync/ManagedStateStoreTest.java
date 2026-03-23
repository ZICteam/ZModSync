package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedStateStoreTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        ManagedStateStore.resetForTests();
    }

    @Test
    void buildStateFileNameUsesSanitizedServerIdPrefix() {
        String fileName = ManagedStateStore.buildStateFileName("mc.example.com:25565");

        assertTrue(fileName.startsWith("mc.example.com_25565-"));
        assertTrue(fileName.endsWith(".json"));
    }

    @Test
    void buildStateFileNameDistinguishesIdsThatSanitizeTheSameWay() {
        String first = ManagedStateStore.buildStateFileName("alpha/beta");
        String second = ManagedStateStore.buildStateFileName("alpha:beta");

        assertNotEquals(first, second);
        assertTrue(first.startsWith("alpha_beta-"));
        assertTrue(second.startsWith("alpha_beta-"));
    }

    @Test
    void buildStateFileNameIsStableForSameServerId() {
        String serverId = "play.example.net:25565";

        assertEquals(
                ManagedStateStore.buildStateFileName(serverId),
                ManagedStateStore.buildStateFileName(serverId)
        );
    }

    @Test
    void saveAndLoadRoundTripManagedEntries() {
        ManagedStateStore.overrideStateRootForTests(tempDir);
        String serverId = "play.example.net:25565";
        List<ManifestEntry> entries = List.of(
                new ManifestEntry(CategoryType.MOD, "example.jar", "example.jar", 12L, "abc", true, true, ""),
                new ManifestEntry(CategoryType.CONFIG, "client/options.txt", "options.txt", 4L, "def", true, false, "")
        );

        ManagedStateStore.save(serverId, entries);
        List<ManifestEntry> loaded = ManagedStateStore.load(serverId);

        assertEquals(2, loaded.size());
        assertEquals(entries.get(0).getIdentityKey(), loaded.get(0).getIdentityKey());
        assertEquals(entries.get(1).getIdentityKey(), loaded.get(1).getIdentityKey());
    }

    @Test
    void loadReturnsEmptyListForInvalidJson() throws Exception {
        ManagedStateStore.overrideStateRootForTests(tempDir);
        String serverId = "play.example.net:25565";
        Path file = tempDir.resolve(ManagedStateStore.buildStateFileName(serverId));
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{not-json", StandardCharsets.UTF_8);

        assertTrue(ManagedStateStore.load(serverId).isEmpty());
    }

    @Test
    void saveAndLoadIgnoreBlankServerIds() {
        ManagedStateStore.overrideStateRootForTests(tempDir);
        List<ManifestEntry> entries = List.of(
                new ManifestEntry(CategoryType.MOD, "example.jar", "example.jar", 12L, "abc", true, true, "")
        );

        ManagedStateStore.save(" ", entries);

        assertTrue(ManagedStateStore.load(" ").isEmpty());
        assertTrue(ManagedStateStore.load(null).isEmpty());
    }
}
