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

class SyncPipelineIntegrationTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        FileHashCache.resetForTests();
    }

    @Test
    void comparatorFlagsChangedAndMissingEntriesBetweenServerManifestAndClientScan() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path manifestCopy = tempDir.resolve("generated/modsync-manifest.json");

        Path serverModsRoot = tempDir.resolve("server/mods");
        Path serverConfigRoot = tempDir.resolve("server/sync_repo/configs");
        Path clientModsRoot = tempDir.resolve("client/mods");
        Path clientConfigRoot = tempDir.resolve("client/config");

        Files.createDirectories(serverModsRoot.resolve("nested"));
        Files.createDirectories(serverConfigRoot.resolve("client"));
        Files.createDirectories(clientModsRoot.resolve("nested"));
        Files.createDirectories(clientConfigRoot.resolve("client"));

        Files.writeString(serverModsRoot.resolve("nested/core.jar"), "server-mod-new");
        Files.writeString(serverConfigRoot.resolve("client/options.txt"), "server-config");

        Files.writeString(clientModsRoot.resolve("nested/core.jar"), "client-mod-old");

        Map<CategoryType, Path> serverRoots = new EnumMap<>(CategoryType.class);
        serverRoots.put(CategoryType.MOD, serverModsRoot);
        serverRoots.put(CategoryType.CONFIG, serverConfigRoot);

        Map<CategoryType, Path> clientRoots = new EnumMap<>(CategoryType.class);
        clientRoots.put(CategoryType.MOD, clientModsRoot);
        clientRoots.put(CategoryType.CONFIG, clientConfigRoot);

        FileHashCache.overrideCachePathForTests(cachePath);
        ManifestData manifest = ManifestGenerator.generateManifest(serverRoots, Set.of(".log"), manifestCopy);

        FileHashCache.overrideCachePathForTests(cachePath);
        List<ManifestEntry> clientEntries = ClientFileScanner.scanLocalFiles("play.example.net:25565", clientRoots, Set.of(".log"));

        List<ManifestEntry> missingOrOutdated = SyncComparator.findMissingOrOutdated(clientEntries, manifest);

        assertEquals(
                List.of("MOD:nested/core.jar", "CONFIG:client/options.txt"),
                missingOrOutdated.stream().map(ManifestEntry::getIdentityKey).toList()
        );
    }

    @Test
    void comparatorReturnsEmptyWhenClientScanMatchesGeneratedManifest() throws Exception {
        Path cachePath = tempDir.resolve("hash-cache.json");
        Path manifestCopy = tempDir.resolve("generated/modsync-manifest.json");

        Path serverModsRoot = tempDir.resolve("shared/mods");
        Path serverResourcepacksRoot = tempDir.resolve("shared/resourcepacks");
        Files.createDirectories(serverModsRoot.resolve("nested"));
        Files.createDirectories(serverResourcepacksRoot.resolve("ui"));

        Files.writeString(serverModsRoot.resolve("nested/core.jar"), "same-mod");
        Files.writeString(serverResourcepacksRoot.resolve("ui/pack.zip"), "same-pack");

        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);
        roots.put(CategoryType.MOD, serverModsRoot);
        roots.put(CategoryType.RESOURCEPACK, serverResourcepacksRoot);

        FileHashCache.overrideCachePathForTests(cachePath);
        ManifestData manifest = ManifestGenerator.generateManifest(roots, Set.of(".log"), manifestCopy);

        FileHashCache.overrideCachePathForTests(cachePath);
        List<ManifestEntry> clientEntries = ClientFileScanner.scanLocalFiles("play.example.net:25565", roots, Set.of(".log"));

        assertEquals(List.of(), SyncComparator.findMissingOrOutdated(clientEntries, manifest));
    }
}
