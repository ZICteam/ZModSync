package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreJoinSyncManagerTest {
    @Test
    void buildSyncPlanContinuesImmediatelyWhenAlreadySynchronizedAndAutoConnectEnabled() {
        ManifestEntry sharedEntry = entry("mods/core.jar", "same-hash");
        ManifestData manifest = manifest(sharedEntry.copy());

        PreJoinSyncManager.PreJoinSyncPlan plan = PreJoinSyncManager.buildSyncPlan(
                List.of(sharedEntry),
                manifest,
                true,
                true
        );

        assertTrue(plan.alreadySynchronized());
        assertEquals(List.of(), plan.requiredEntries());
        assertFalse(plan.downloadRequiredButSkipped());
        assertTrue(plan.continueImmediately());
        assertFalse(plan.continueAfterDownloads());
    }

    @Test
    void buildSyncPlanStaysIdleWhenAlreadySynchronizedAndAutoConnectDisabled() {
        ManifestEntry sharedEntry = entry("mods/core.jar", "same-hash");
        ManifestData manifest = manifest(sharedEntry.copy());

        PreJoinSyncManager.PreJoinSyncPlan plan = PreJoinSyncManager.buildSyncPlan(
                List.of(sharedEntry),
                manifest,
                false,
                true
        );

        assertTrue(plan.alreadySynchronized());
        assertEquals(List.of(), plan.requiredEntries());
        assertFalse(plan.downloadRequiredButSkipped());
        assertFalse(plan.continueImmediately());
        assertFalse(plan.continueAfterDownloads());
    }

    @Test
    void buildSyncPlanSchedulesDownloadsAndContinuesAfterCompletionWhenAutoConnectEnabled() {
        ManifestEntry localEntry = entry("mods/core.jar", "local-hash");
        ManifestEntry serverEntry = entry("mods/core.jar", "server-hash");
        ManifestData manifest = manifest(serverEntry);

        PreJoinSyncManager.PreJoinSyncPlan plan = PreJoinSyncManager.buildSyncPlan(
                List.of(localEntry),
                manifest,
                true,
                true
        );

        assertFalse(plan.alreadySynchronized());
        assertEquals(List.of("MOD:mods/core.jar"),
                plan.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        assertFalse(plan.downloadRequiredButSkipped());
        assertFalse(plan.continueImmediately());
        assertTrue(plan.continueAfterDownloads());
    }

    @Test
    void buildSyncPlanSchedulesDownloadsWithoutContinuationWhenAutoConnectDisabled() {
        ManifestData manifest = manifest(entry("mods/core.jar", "server-hash"));

        PreJoinSyncManager.PreJoinSyncPlan plan = PreJoinSyncManager.buildSyncPlan(
                List.of(),
                manifest,
                false,
                true
        );

        assertFalse(plan.alreadySynchronized());
        assertEquals(List.of("MOD:mods/core.jar"),
                plan.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        assertFalse(plan.downloadRequiredButSkipped());
        assertFalse(plan.continueImmediately());
        assertFalse(plan.continueAfterDownloads());
    }

    @Test
    void buildSyncPlanBlocksConnectionWithoutDownloadsWhenClientIsOutdated() {
        ManifestData manifest = manifest(entry("mods/core.jar", "server-hash"));

        PreJoinSyncManager.PreJoinSyncPlan plan = PreJoinSyncManager.buildSyncPlan(
                List.of(),
                manifest,
                true,
                false
        );

        assertFalse(plan.alreadySynchronized());
        assertTrue(plan.downloadRequiredButSkipped());
        assertFalse(plan.continueImmediately());
        assertFalse(plan.continueAfterDownloads());
    }

    private static ManifestData manifest(ManifestEntry... entries) {
        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(entries));
        return manifest;
    }

    private static ManifestEntry entry(String relativePath, String sha256) {
        return new ManifestEntry(
                CategoryType.MOD,
                relativePath,
                "core.jar",
                123L,
                sha256,
                true,
                true,
                "https://cdn.example.com/files/mod/core.jar"
        );
    }
}
