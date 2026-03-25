package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakeManifestPipelineTest {
    @Test
    void packetHandshakePipelineCarriesEncodedUrlsIntoClientDownloadDecision() {
        ManifestData serverManifest = new ManifestData();
        serverManifest.setGeneratedAt(123456L);
        serverManifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/My Cool+Mod 100%.jar",
                        "My Cool+Mod 100%.jar",
                        12L,
                        "server-mod-hash",
                        true,
                        true,
                        ""
                ),
                new ManifestEntry(
                        CategoryType.CONFIG,
                        "client/options.txt",
                        "options.txt",
                        4L,
                        "same-config-hash",
                        true,
                        false,
                        ""
                )
        ));

        List<ManifestEntry> clientEntries = List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/My Cool+Mod 100%.jar",
                        "My Cool+Mod 100%.jar",
                        12L,
                        "client-mod-hash",
                        true,
                        true,
                        ""
                ),
                new ManifestEntry(
                        CategoryType.CONFIG,
                        "client/options.txt",
                        "options.txt",
                        4L,
                        "same-config-hash",
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

        assertEquals(
                "https://cdn.example.com/modsync/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                serverResponse.outboundManifest().getEntries().get(0).getDownloadUrl()
        );
        assertEquals(List.of("MOD:mods/My Cool+Mod 100%.jar"),
                serverResponse.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());

        HandshakeManifestPlanner.StartDownloadPlan clientPlan =
                HandshakeManifestPlanner.buildStartDownloadPlan(
                        serverResponse.requiredEntries(),
                        serverResponse.outboundManifest(),
                        clientEntries
                );

        assertFalse(clientPlan.alreadySynchronized());
        assertTrue(clientPlan.saveManagedManifest());
        assertTrue(clientPlan.startDownloads());
        assertEquals(List.of("MOD:mods/My Cool+Mod 100%.jar"),
                clientPlan.requiredEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        assertEquals(
                "https://cdn.example.com/modsync/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                clientPlan.requiredEntries().get(0).getDownloadUrl()
        );
    }

    @Test
    void packetHandshakePipelineStaysIdleWhenClientAlreadyMatchesServerManifest() {
        ManifestData serverManifest = new ManifestData();
        serverManifest.setGeneratedAt(654321L);
        serverManifest.setEntries(List.of(
                new ManifestEntry(
                        CategoryType.RESOURCEPACK,
                        "ui/pack.zip",
                        "pack.zip",
                        10L,
                        "same-pack-hash",
                        true,
                        false,
                        ""
                )
        ));

        List<ManifestEntry> clientEntries = List.of(
                new ManifestEntry(
                        CategoryType.RESOURCEPACK,
                        "ui/pack.zip",
                        "pack.zip",
                        10L,
                        "same-pack-hash",
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

        HandshakeManifestPlanner.StartDownloadPlan clientPlan =
                HandshakeManifestPlanner.buildStartDownloadPlan(
                        serverResponse.requiredEntries(),
                        serverResponse.outboundManifest(),
                        clientEntries
                );

        assertTrue(serverResponse.requiredEntries().isEmpty());
        assertTrue(clientPlan.alreadySynchronized());
        assertTrue(clientPlan.saveManagedManifest());
        assertFalse(clientPlan.startDownloads());
        assertEquals(List.of(), clientPlan.requiredEntries());
    }

    @Test
    void packetHandshakePipelineHandlesMissingServerManifestCacheAsEmptyResponse() {
        List<ManifestEntry> clientEntries = List.of(
                new ManifestEntry(
                        CategoryType.MOD,
                        "mods/local-only.jar",
                        "local-only.jar",
                        12L,
                        "local-hash",
                        true,
                        true,
                        ""
                )
        );

        ManifestData serverManifest = HandshakeManifestPlanner.ensureServerManifest(null, 777L);
        HandshakeManifestPlanner.ServerHandshakeResponse serverResponse =
                HandshakeManifestPlanner.buildServerHandshakeResponse(
                        clientEntries,
                        serverManifest,
                        "https://cdn.example.com/modsync"
                );

        HandshakeManifestPlanner.StartDownloadPlan clientPlan =
                HandshakeManifestPlanner.buildStartDownloadPlan(
                        serverResponse.requiredEntries(),
                        serverResponse.outboundManifest(),
                        clientEntries
                );

        assertEquals(777L, serverResponse.outboundManifest().getGeneratedAt());
        assertEquals(List.of(), serverResponse.outboundManifest().getEntries());
        assertEquals(List.of(), serverResponse.requiredEntries());
        assertTrue(clientPlan.alreadySynchronized());
        assertTrue(clientPlan.saveManagedManifest());
        assertFalse(clientPlan.startDownloads());
        assertEquals(List.of(), clientPlan.requiredEntries());
    }
}
