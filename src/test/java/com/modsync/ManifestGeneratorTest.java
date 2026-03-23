package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestGeneratorTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        FileHashCache.resetForTests();
    }

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

    @Test
    void generateManifestBuildsEntriesAndWritesManifestCopy() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path manifestCopy = tempDir.resolve("generated/modsync-manifest.json");
        Path modsRoot = tempDir.resolve("mods");
        Path resourcepacksRoot = tempDir.resolve("sync_repo/resourcepacks");
        Files.createDirectories(modsRoot.resolve("nested"));
        Files.createDirectories(resourcepacksRoot.resolve("ui"));
        Files.writeString(modsRoot.resolve("nested/core.jar"), "core-binary");
        Files.writeString(resourcepacksRoot.resolve("ui/pack.zip"), "pack-binary");

        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.MOD, modsRoot);
        roots.put(CategoryType.RESOURCEPACK, resourcepacksRoot);

        FileHashCache.overrideCachePathForTests(cachePath);
        ManifestData manifest = ManifestGenerator.generateManifest(roots, Set.of(".log"), manifestCopy);

        assertEquals(2, manifest.getEntries().size());
        assertTrue(manifest.getGeneratedAt() > 0);
        assertTrue(manifest.getEntries().stream().anyMatch(entry ->
                entry.getCategory() == CategoryType.MOD
                        && entry.getRelativePath().equals("nested/core.jar")
                        && entry.getFileName().equals("core.jar")
                        && entry.isRestartRequired()
                        && entry.getSha256().matches("[0-9a-f]{64}")
        ));
        assertTrue(manifest.getEntries().stream().anyMatch(entry ->
                entry.getCategory() == CategoryType.RESOURCEPACK
                        && entry.getRelativePath().equals("ui/pack.zip")
                        && !entry.isRestartRequired()
        ));
        assertTrue(Files.exists(manifestCopy));
        assertTrue(Files.readString(manifestCopy).contains("\"relativePath\": \"nested/core.jar\""));
    }

    @Test
    void generateManifestSkipsConfiguredExtensionsAndMissingRoots() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path manifestCopy = tempDir.resolve("generated/modsync-manifest.json");
        Path modsRoot = tempDir.resolve("mods");
        Files.createDirectories(modsRoot);
        Files.writeString(modsRoot.resolve("keep.jar"), "jar");
        Files.writeString(modsRoot.resolve("skip.LOG"), "log");

        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.MOD, modsRoot);
        roots.put(CategoryType.CONFIG, tempDir.resolve("missing-config"));

        FileHashCache.overrideCachePathForTests(cachePath);
        ManifestData manifest = ManifestGenerator.generateManifest(roots, Set.of(".log"), manifestCopy);

        assertEquals(1, manifest.getEntries().size());
        assertEquals("keep.jar", manifest.getEntries().get(0).getRelativePath());
    }
}
