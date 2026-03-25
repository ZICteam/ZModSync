package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketHandshakeChunkingIntegrationTest {
    @Test
    void chunkedManifestAndStartDownloadPayloadPreserveEncodedPathsAcrossRoundTrip() {
        ManifestData serverManifest = new ManifestData();
        serverManifest.setGeneratedAt(123456L);
        serverManifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/" + "Very Long Name ".repeat(12).trim() + "+100%.jar",
                        "Very Long Name ".repeat(12).trim() + "+100%.jar",
                        12L,
                        "server-hash",
                        true,
                        true,
                        ""
                )
        ));

        List<ManifestEntry> clientEntries = List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/" + "Very Long Name ".repeat(12).trim() + "+100%.jar",
                        "Very Long Name ".repeat(12).trim() + "+100%.jar",
                        12L,
                        "client-hash",
                        true,
                        true,
                        ""
                )
        );

        HandshakeManifestPlanner.ServerHandshakeResponse serverResponse =
                HandshakeManifestPlanner.buildServerHandshakeResponse(
                        clientEntries,
                        serverManifest,
                        "https://cdn.example.com/modsync"
                );

        String manifestJson = ManifestGenerator.toJson(serverResponse.outboundManifest());
        String requiredJson = ManifestGenerator.entriesToJson(serverResponse.requiredEntries());

        ManifestData reassembledManifest = ManifestGenerator.fromJson(
                reassemble(manifestJson, 17)
        );
        List<ManifestEntry> reassembledRequired = ManifestGenerator.entriesFromJson(
                reassemble(requiredJson, 11)
        );

        HandshakeManifestPlanner.StartDownloadPlan clientPlan =
                HandshakeManifestPlanner.buildStartDownloadPlan(
                        reassembledRequired,
                        reassembledManifest,
                        clientEntries
                );

        assertEquals(List.of("MOD:" + serverManifest.getEntries().get(0).getRelativePath()),
                reassembledRequired.stream().map(ManifestEntry::getIdentityKey).toList());
        assertEquals(
                "https://cdn.example.com/modsync/files/mod/"
                        + HttpPathCodec.encodeRelativePath(serverManifest.getEntries().get(0).getRelativePath()),
                reassembledRequired.get(0).getDownloadUrl()
        );
        assertFalse(clientPlan.alreadySynchronized());
        assertTrue(clientPlan.startDownloads());
        assertEquals(reassembledRequired.get(0).getDownloadUrl(), clientPlan.requiredEntries().get(0).getDownloadUrl());
    }

    @Test
    void chunkedClientDecisionIgnoresStaleServerSuggestedPayloadWhenChunkedManifestShowsNoChanges() {
        ManifestData serverManifest = new ManifestData();
        serverManifest.setGeneratedAt(222333L);
        serverManifest.setEntries(List.of(
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

        List<ManifestEntry> clientEntries = List.of(
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
        );

        HandshakeManifestPlanner.ServerHandshakeResponse serverResponse =
                HandshakeManifestPlanner.buildServerHandshakeResponse(
                        clientEntries,
                        serverManifest,
                        "https://cdn.example.com/modsync"
                );

        String manifestJson = ManifestGenerator.toJson(serverResponse.outboundManifest());
        String staleSuggestedJson = ManifestGenerator.entriesToJson(List.of(
                new ManifestEntry(
                        CategoryType.CONFIG,
                        "client/stale.txt",
                        "stale.txt",
                        4L,
                        "stale",
                        true,
                        false,
                        "https://cdn.example.com/modsync/files/config/client/stale.txt"
                )
        ));

        ManifestData reassembledManifest = ManifestGenerator.fromJson(reassemble(manifestJson, 9));
        List<ManifestEntry> reassembledStaleSuggested = ManifestGenerator.entriesFromJson(reassemble(staleSuggestedJson, 7));

        HandshakeManifestPlanner.StartDownloadPlan clientPlan =
                HandshakeManifestPlanner.buildStartDownloadPlan(
                        reassembledStaleSuggested,
                        reassembledManifest,
                        clientEntries
                );

        assertEquals(List.of("CONFIG:client/stale.txt"),
                reassembledStaleSuggested.stream().map(ManifestEntry::getIdentityKey).toList());
        assertTrue(clientPlan.alreadySynchronized());
        assertTrue(clientPlan.saveManagedManifest());
        assertFalse(clientPlan.startDownloads());
        assertEquals(List.of(), clientPlan.requiredEntries());
    }

    private static String reassemble(String payload, int chunkSize) {
        ChunkedPayloadCodec.ChunkAccumulator accumulator = new ChunkedPayloadCodec.ChunkAccumulator();
        List<String> chunks = ChunkedPayloadCodec.split(payload, chunkSize);
        String result = null;
        for (int i = 0; i < chunks.size(); i++) {
            result = accumulator.accept(i, chunks.size(), chunks.get(i));
        }
        return result;
    }
}
