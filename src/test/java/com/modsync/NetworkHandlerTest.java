package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkHandlerTest {
    @Test
    void attachDownloadUrlsEncodesRelativePathsForPacketBasedManifestFlow() {
        ManifestData manifest = new ManifestData();
        manifest.setGeneratedAt(123L);
        manifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/My Cool+Mod 100%.jar",
                        "My Cool+Mod 100%.jar",
                        12L,
                        "abc",
                        true,
                        true,
                        ""
                )
        ));

        ManifestData outbound = HandshakeManifestPlanner.attachDownloadUrls(manifest, "https://cdn.example.com/modsync");

        assertEquals(123L, outbound.getGeneratedAt());
        assertEquals(
                "https://cdn.example.com/modsync/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                outbound.getEntries().get(0).getDownloadUrl()
        );
        assertEquals("", manifest.getEntries().get(0).getDownloadUrl());
    }

    @Test
    void determineRequiredDownloadsUsesLocalComparisonWhenManifestIsAvailable() {
        ManifestEntry serverManifestEntry = new ManifestEntry(
                CategoryType.MOD,
                "mods/core.jar",
                "core.jar",
                12L,
                "server-hash",
                true,
                true,
                "https://cdn.example.com/files/mod/mods/core.jar"
        );
        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(serverManifestEntry));

        List<ManifestEntry> result = HandshakeManifestPlanner.determineRequiredDownloads(
                List.of(
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/old-suggested.jar",
                                "old-suggested.jar",
                                12L,
                                "old",
                                true,
                                true,
                                "https://cdn.example.com/files/mod/mods/old-suggested.jar"
                        )
                ),
                manifest,
                List.of(
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/core.jar",
                                "core.jar",
                                12L,
                                "client-hash",
                                true,
                                true,
                                ""
                        )
                )
        );

        assertEquals(List.of("MOD:mods/core.jar"),
                result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void determineRequiredDownloadsFallsBackToServerSuggestedListWithoutManifest() {
        List<ManifestEntry> serverSuggested = List.of(
                new ManifestEntry(
                        CategoryType.CONFIG,
                        "client/options.txt",
                        "options.txt",
                        4L,
                        "abc",
                        true,
                        false,
                        "https://cdn.example.com/files/config/client/options.txt"
                )
        );

        List<ManifestEntry> result = HandshakeManifestPlanner.determineRequiredDownloads(
                serverSuggested,
                null,
                List.of()
        );

        assertEquals(serverSuggested, result);
    }

    @Test
    void ensureServerManifestReturnsExistingManifestUnchanged() {
        ManifestData existing = new ManifestData();
        existing.setGeneratedAt(555L);
        existing.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/core.jar",
                        "core.jar",
                        12L,
                        "hash",
                        true,
                        true,
                        ""
                )
        ));

        ManifestData result = HandshakeManifestPlanner.ensureServerManifest(existing, 999L);

        assertEquals(existing, result);
        assertEquals(555L, result.getGeneratedAt());
        assertEquals(1, result.getEntries().size());
    }

    @Test
    void ensureServerManifestCreatesEmptyManifestWhenCacheIsMissing() {
        ManifestData result = HandshakeManifestPlanner.ensureServerManifest(null, 777L);

        assertEquals(777L, result.getGeneratedAt());
        assertEquals(List.of(), result.getEntries());
    }

    @Test
    void buildServerHandshakeResponseAddsEncodedUrlsAndComputesRequiredEntriesFromOutboundManifest() {
        ManifestData manifest = new ManifestData();
        manifest.setGeneratedAt(321L);
        manifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/My Cool+Mod 100%.jar",
                        "My Cool+Mod 100%.jar",
                        12L,
                        "server-hash",
                        true,
                        true,
                        ""
                ),
                new ManifestEntry(
                        CategoryType.CONFIG,
                        "client/options.txt",
                        "options.txt",
                        4L,
                        "same-hash",
                        true,
                        false,
                        ""
                )
        ));

        HandshakeManifestPlanner.ServerHandshakeResponse response =
                HandshakeManifestPlanner.buildServerHandshakeResponse(
                        List.of(
                                new ManifestEntry(
                                        CategoryType.MOD,
                                        "mods/My Cool+Mod 100%.jar",
                                        "My Cool+Mod 100%.jar",
                                        12L,
                                        "client-hash",
                                        true,
                                        true,
                                        ""
                                ),
                                new ManifestEntry(
                                        CategoryType.CONFIG,
                                        "client/options.txt",
                                        "options.txt",
                                        4L,
                                        "same-hash",
                                        true,
                                        false,
                                        ""
                                )
                        ),
                        manifest,
                        "https://cdn.example.com/modsync"
                );

        assertEquals(321L, response.outboundManifest().getGeneratedAt());
        assertEquals(
                "https://cdn.example.com/modsync/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                response.outboundManifest().getEntries().get(0).getDownloadUrl()
        );
        assertEquals(List.of("MOD:mods/My Cool+Mod 100%.jar"),
                response.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void buildStartDownloadPlanMarksClientAsSynchronizedWhenNothingNeedsDownload() {
        ManifestEntry manifestEntry = new ManifestEntry(
                CategoryType.CONFIG,
                "client/options.txt",
                "options.txt",
                4L,
                "same-hash",
                true,
                false,
                "https://cdn.example.com/files/config/client/options.txt"
        );
        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(manifestEntry));

        HandshakeManifestPlanner.StartDownloadPlan plan = HandshakeManifestPlanner.buildStartDownloadPlan(
                List.of(
                        new ManifestEntry(
                                CategoryType.CONFIG,
                                "client/stale-server-suggested.txt",
                                "stale-server-suggested.txt",
                                4L,
                                "stale",
                                true,
                                false,
                                "https://cdn.example.com/files/config/client/stale-server-suggested.txt"
                        )
                ),
                manifest,
                List.of(
                        new ManifestEntry(
                                CategoryType.CONFIG,
                                "client/options.txt",
                                "options.txt",
                                4L,
                                "same-hash",
                                true,
                                false,
                                ""
                        )
                )
        );

        assertTrue(plan.alreadySynchronized());
        assertTrue(plan.saveManagedManifest());
        assertFalse(plan.startDownloads());
        assertEquals(List.of(), plan.requiredEntries());
    }

    @Test
    void buildStartDownloadPlanStartsDownloadsWhenManifestComparisonFindsChanges() {
        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/core.jar",
                        "core.jar",
                        12L,
                        "server-hash",
                        true,
                        true,
                        "https://cdn.example.com/files/mod/mods/core.jar"
                )
        ));

        HandshakeManifestPlanner.StartDownloadPlan plan = HandshakeManifestPlanner.buildStartDownloadPlan(
                List.of(),
                manifest,
                List.of(
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/core.jar",
                                "core.jar",
                                12L,
                                "client-hash",
                                true,
                                true,
                                ""
                        )
                )
        );

        assertFalse(plan.alreadySynchronized());
        assertTrue(plan.saveManagedManifest());
        assertTrue(plan.startDownloads());
        assertEquals(List.of("MOD:mods/core.jar"),
                plan.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void buildStartDownloadPlanUsesServerSuggestedListWhenManifestIsMissing() {
        List<ManifestEntry> serverSuggested = List.of(
                new ManifestEntry(
                        CategoryType.RESOURCEPACK,
                        "ui/pack.zip",
                        "pack.zip",
                        10L,
                        "hash",
                        true,
                        false,
                        "https://cdn.example.com/files/resourcepack/ui/pack.zip"
                )
        );

        HandshakeManifestPlanner.StartDownloadPlan plan = HandshakeManifestPlanner.buildStartDownloadPlan(
                serverSuggested,
                null,
                List.of()
        );

        assertFalse(plan.alreadySynchronized());
        assertFalse(plan.saveManagedManifest());
        assertTrue(plan.startDownloads());
        assertEquals(serverSuggested, plan.requiredEntries());
    }

    @Test
    void determineRequiredDownloadsFiltersUndownloadableServerSuggestedEntriesWithoutManifest() {
        List<ManifestEntry> result = HandshakeManifestPlanner.determineRequiredDownloads(
                List.of(
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/missing-url.jar",
                                "missing-url.jar",
                                12L,
                                "hash-a",
                                true,
                                true,
                                ""
                        ),
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/missing-hash.jar",
                                "missing-hash.jar",
                                12L,
                                "",
                                true,
                                true,
                                "https://cdn.example.com/files/mod/mods/missing-hash.jar"
                        ),
                        new ManifestEntry(
                                CategoryType.MOD,
                                "mods/valid.jar",
                                "valid.jar",
                                12L,
                                "hash-valid",
                                true,
                                true,
                                "https://cdn.example.com/files/mod/mods/valid.jar"
                        )
                ),
                null,
                List.of()
        );

        assertEquals(List.of("MOD:mods/valid.jar"),
                result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void buildStartDownloadPlanDoesNotStartDownloadsWhenOnlyUndownloadableFallbackEntriesRemain() {
        HandshakeManifestPlanner.StartDownloadPlan plan = HandshakeManifestPlanner.buildStartDownloadPlan(
                List.of(
                        new ManifestEntry(
                                CategoryType.CONFIG,
                                "client/bad.txt",
                                "bad.txt",
                                1L,
                                "hash",
                                true,
                                false,
                                ""
                        )
                ),
                null,
                List.of()
        );

        assertTrue(plan.alreadySynchronized());
        assertFalse(plan.saveManagedManifest());
        assertFalse(plan.startDownloads());
        assertEquals(List.of(), plan.requiredEntries());
    }
}
