package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHashCacheTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        FileHashCache.resetForTests();
    }

    @Test
    void describePersistsAndReloadsFingerprintAcrossSessions() throws Exception {
        Path cachePath = tempDir.resolve("modsync-hash-cache.json");
        Path file = tempDir.resolve("mods/example.jar");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "alpha");

        FileHashCache.overrideCachePathForTests(cachePath);
        FileHashCache.FileFingerprint first;
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            first = session.describe(file, "example.jar");
        }

        FileHashCache.overrideCachePathForTests(cachePath);
        FileHashCache.FileFingerprint second;
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            second = session.describe(file, "example.jar");
        }

        assertEquals(first, second);
        assertTrue(Files.exists(cachePath));
        assertTrue(Files.readString(cachePath).contains("\"example.jar\""));
    }

    @Test
    void describeRefreshesCacheWhenFileChanges() throws Exception {
        Path cachePath = tempDir.resolve("modsync-hash-cache.json");
        Path file = tempDir.resolve("mods/example.jar");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "alpha");

        FileHashCache.overrideCachePathForTests(cachePath);
        FileHashCache.FileFingerprint first;
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            first = session.describe(file, "example.jar");
        }

        Files.writeString(file, "beta-updated");
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 2_000));

        FileHashCache.overrideCachePathForTests(cachePath);
        FileHashCache.FileFingerprint second;
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            second = session.describe(file, "example.jar");
        }

        assertNotEquals(first.size(), second.size());
        assertNotEquals(first.sha256(), second.sha256());
    }

    @Test
    void closeRemovesEntriesThatWereNotSeenInCurrentScopeSession() throws Exception {
        Path cachePath = tempDir.resolve("modsync-hash-cache.json");
        Path firstFile = tempDir.resolve("mods/first.jar");
        Path secondFile = tempDir.resolve("mods/second.jar");
        Files.createDirectories(firstFile.getParent());
        Files.writeString(firstFile, "first");
        Files.writeString(secondFile, "second");

        FileHashCache.overrideCachePathForTests(cachePath);
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            session.describe(firstFile, "first.jar");
            session.describe(secondFile, "second.jar");
        }

        FileHashCache.overrideCachePathForTests(cachePath);
        try (FileHashCache.ScopeSession session = FileHashCache.openServerScope(CategoryType.MOD)) {
            session.describe(firstFile, "first.jar");
        }

        String persisted = Files.readString(cachePath);
        assertTrue(persisted.contains("\"first.jar\""));
        assertFalse(persisted.contains("\"second.jar\""));
    }

    @Test
    void clientAndServerScopesStaySeparatedInPersistedCache() throws Exception {
        Path cachePath = tempDir.resolve("modsync-hash-cache.json");
        Path file = tempDir.resolve("mods/shared-name.jar");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "shared");

        FileHashCache.overrideCachePathForTests(cachePath);
        try (FileHashCache.ScopeSession server = FileHashCache.openServerScope(CategoryType.MOD);
             FileHashCache.ScopeSession client = FileHashCache.openClientScope("play.example.net:25565", CategoryType.MOD)) {
            server.describe(file, "shared-name.jar");
            client.describe(file, "shared-name.jar");
        }

        String persisted = Files.readString(cachePath);
        assertTrue(persisted.contains("\"server:MOD\""));
        assertTrue(persisted.contains("\"client:play.example.net_25565:MOD\""));
    }
}
