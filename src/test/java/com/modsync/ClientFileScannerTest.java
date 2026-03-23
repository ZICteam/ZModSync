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

class ClientFileScannerTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        FileHashCache.resetForTests();
    }

    @Test
    void scanLocalFilesBuildsManifestEntriesFromProvidedRoots() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path modsRoot = tempDir.resolve("mods");
        Path configRoot = tempDir.resolve("config");
        Files.createDirectories(modsRoot.resolve("nested"));
        Files.createDirectories(configRoot.resolve("client"));
        Files.writeString(modsRoot.resolve("nested/example.jar"), "mod-binary");
        Files.writeString(configRoot.resolve("client/options.txt"), "config-body");

        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.MOD, modsRoot);
        roots.put(CategoryType.CONFIG, configRoot);

        FileHashCache.overrideCachePathForTests(cachePath);
        List<ManifestEntry> entries = ClientFileScanner.scanLocalFiles("play.example.net:25565", roots, Set.of(".log"));

        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(entry ->
                entry.getCategory() == CategoryType.MOD
                        && entry.getRelativePath().equals("nested/example.jar")
                        && entry.getFileName().equals("example.jar")
                        && entry.getFileSize() > 0
                        && entry.getSha256().matches("[0-9a-f]{64}")
                        && entry.isRestartRequired()
        ));
        assertTrue(entries.stream().anyMatch(entry ->
                entry.getCategory() == CategoryType.CONFIG
                        && entry.getRelativePath().equals("client/options.txt")
                        && entry.getFileName().equals("options.txt")
                        && !entry.isRestartRequired()
        ));
    }

    @Test
    void scanLocalFilesSkipsConfiguredExtensionsAndMissingRoots() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path modsRoot = tempDir.resolve("mods");
        Files.createDirectories(modsRoot);
        Files.writeString(modsRoot.resolve("keep.jar"), "jar");
        Files.writeString(modsRoot.resolve("skip.LOG"), "log");

        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.MOD, modsRoot);
        roots.put(CategoryType.RESOURCEPACK, tempDir.resolve("missing-resourcepacks"));

        FileHashCache.overrideCachePathForTests(cachePath);
        List<ManifestEntry> entries = ClientFileScanner.scanLocalFiles("play.example.net:25565", roots, Set.of(".log"));

        assertEquals(1, entries.size());
        assertEquals("keep.jar", entries.get(0).getRelativePath());
    }
}
