package com.modsync;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerSyncStatusCacheTest {
    @AfterEach
    void tearDown() {
        ServerSyncStatusCache.resetForTests();
    }

    @Test
    void shouldRefreshWhenServerHasNoCachedStatus() {
        assertTrue(ServerSyncStatusCache.shouldRefresh("play.example.net:25565"));
    }

    @Test
    void shouldNotRefreshWhileStatusIsChecking() {
        ServerSyncStatusCache.putStatusForTests("play.example.net:25565", ServerSyncStatusCache.SyncState.CHECKING, System.currentTimeMillis());

        assertFalse(ServerSyncStatusCache.shouldRefresh("play.example.net:25565"));
    }

    @Test
    void shouldRefreshWhenStatusSnapshotIsOlderThanThreshold() {
        long oldTimestamp = System.currentTimeMillis() - 121_000L;
        ServerSyncStatusCache.putStatusForTests("play.example.net:25565", ServerSyncStatusCache.SyncState.SYNCED, oldTimestamp);

        assertTrue(ServerSyncStatusCache.shouldRefresh("play.example.net:25565"));
    }

    @Test
    void shouldNotRefreshWhenRecentStableStatusExists() {
        ServerSyncStatusCache.putStatusForTests("play.example.net:25565", ServerSyncStatusCache.SyncState.OUTDATED, System.currentTimeMillis());

        assertFalse(ServerSyncStatusCache.shouldRefresh("play.example.net:25565"));
    }

    @Test
    void cacheLocalEntriesUsesServerIdAsLookupKey() {
        List<ManifestEntry> entries = List.of(entry(CategoryType.MOD, "mods/a.jar"));
        ServerSyncStatusCache.cacheLocalEntries("play.example.net:25565", entries);

        List<ManifestEntry> cached = ServerSyncStatusCache.getCachedOrScanLocalEntries("play.example.net:25565");

        assertEquals(1, cached.size());
        assertEquals("MOD:mods/a.jar", cached.get(0).getIdentityKey());
    }

    @Test
    void markDirtyClearsCachedStatusForServer() {
        String serverId = "play.example.net:25565";
        ServerSyncStatusCache.putStatusForTests(serverId, ServerSyncStatusCache.SyncState.SYNCED, System.currentTimeMillis());

        assertFalse(ServerSyncStatusCache.shouldRefresh(serverId));

        ServerSyncStatusCache.markDirty(serverId);

        assertTrue(ServerSyncStatusCache.shouldRefresh(serverId));
    }

    private static ManifestEntry entry(CategoryType category, String relativePath) {
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        return new ManifestEntry(category, relativePath, fileName, 10L, "aaa", true, category.isDefaultRestartRequired(), "");
    }
}
