package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManifestGeneratorTest {
    @Test
    void manifestJsonRoundTripPreservesEntries() {
        ManifestData manifest = new ManifestData();
        manifest.setGeneratedAt(123456789L);
        manifest.setEntries(List.of(
                new ManifestEntry(CategoryType.MOD, "mods/core.jar", "core.jar", 100L, "abc", true, true, "http://example/mods/core.jar"),
                new ManifestEntry(CategoryType.CONFIG, "client/options.txt", "options.txt", 200L, "def", true, false, "http://example/config/options.txt")
        ));

        ManifestData restored = ManifestGenerator.fromJson(ManifestGenerator.toJson(manifest));

        assertEquals(123456789L, restored.getGeneratedAt());
        assertEquals(2, restored.getEntries().size());
        assertEquals("MOD:mods/core.jar", restored.getEntries().get(0).getIdentityKey());
        assertEquals("http://example/config/options.txt", restored.getEntries().get(1).getDownloadUrl());
    }

    @Test
    void entriesJsonRoundTripPreservesIdentityKeys() {
        List<ManifestEntry> entries = List.of(
                new ManifestEntry(CategoryType.MOD, "mods/a.jar", "a.jar", 10L, "aaa", true, true, ""),
                new ManifestEntry(CategoryType.RESOURCEPACK, "packs/ui.zip", "ui.zip", 20L, "bbb", false, false, "")
        );

        List<ManifestEntry> restored = ManifestGenerator.entriesFromJson(ManifestGenerator.entriesToJson(entries));

        assertEquals(List.of("MOD:mods/a.jar", "RESOURCEPACK:packs/ui.zip"),
                restored.stream().map(ManifestEntry::getIdentityKey).toList());
    }
}
