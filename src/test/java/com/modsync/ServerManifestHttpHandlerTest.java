package com.modsync;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerManifestHttpHandlerTest {
    @Test
    void buildPublicManifestCopiesEntriesAndAddsDownloadUrls() {
        ManifestData manifest = manifest(List.of(
                new ManifestEntry(CategoryType.MOD, "example.jar", "example.jar", 12L, "abc", true, true, ""),
                new ManifestEntry(CategoryType.CONFIG, "client/options.txt", "options.txt", 4L, "def", true, false, "")
        ));

        ManifestData publicManifest = ServerManifestHttpHandler.buildPublicManifest(manifest, "https://cdn.example.com/modsync");

        assertEquals(2, publicManifest.getEntries().size());
        assertEquals("https://cdn.example.com/modsync/files/mod/example.jar",
                publicManifest.getEntries().get(0).getDownloadUrl());
        assertEquals("https://cdn.example.com/modsync/files/config/client/options.txt",
                publicManifest.getEntries().get(1).getDownloadUrl());
        assertEquals("", manifest.getEntries().get(0).getDownloadUrl());
    }

    @Test
    void buildPublicManifestHandlesNullManifestAsEmpty() {
        ManifestData publicManifest = ServerManifestHttpHandler.buildPublicManifest(null, "http://127.0.0.1:8080");

        assertEquals(0L, publicManifest.getGeneratedAt());
        assertTrue(publicManifest.getEntries().isEmpty());
    }

    @Test
    void fetchManifestFromCandidatesParsesManifestFromLocalHttpServer() throws Exception {
        ManifestData manifest = manifest(List.of(
                new ManifestEntry(CategoryType.MOD, "mods/example.jar", "example.jar", 12L, "abc", true, true, "http://example/mods/example.jar")
        ));
        HttpServer server = jsonServer(200, ManifestGenerator.toJson(manifest));
        server.start();

        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/manifest";

            ManifestData fetched = ServerManifestHttpHandler.fetchManifestFromCandidates(List.of(url), 2_000, 2_000);

            assertEquals(1, fetched.getEntries().size());
            assertEquals("MOD:mods/example.jar", fetched.getEntries().get(0).getIdentityKey());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchManifestFromCandidatesFallsBackAfterFailingUrl() throws Exception {
        HttpServer failing = jsonServer(404, "");
        HttpServer succeeding = jsonServer(200, ManifestGenerator.toJson(manifest(List.of(
                new ManifestEntry(CategoryType.CONFIG, "client/options.txt", "options.txt", 4L, "def", true, false, "")
        ))));
        failing.start();
        succeeding.start();

        try {
            String failingUrl = "http://127.0.0.1:" + failing.getAddress().getPort() + "/manifest";
            String succeedingUrl = "http://127.0.0.1:" + succeeding.getAddress().getPort() + "/manifest";

            ManifestData fetched = ServerManifestHttpHandler.fetchManifestFromCandidates(List.of(failingUrl, succeedingUrl), 2_000, 2_000);

            assertEquals(List.of("CONFIG:client/options.txt"),
                    fetched.getEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        } finally {
            failing.stop(0);
            succeeding.stop(0);
        }
    }

    @Test
    void fetchManifestFromCandidatesReportsAttemptedUrlsOnTotalFailure() throws Exception {
        HttpServer first = jsonServer(404, "");
        HttpServer second = jsonServer(500, "");
        first.start();
        second.start();

        try {
            String firstUrl = "http://127.0.0.1:" + first.getAddress().getPort() + "/manifest";
            String secondUrl = "http://127.0.0.1:" + second.getAddress().getPort() + "/manifest";

            IOException exception = assertThrows(IOException.class,
                    () -> ServerManifestHttpHandler.fetchManifestFromCandidates(List.of(firstUrl, secondUrl), 2_000, 2_000));

            assertEquals(true, exception.getMessage().contains(firstUrl));
            assertEquals(true, exception.getMessage().contains(secondUrl));
        } finally {
            first.stop(0);
            second.stop(0);
        }
    }

    @Test
    void fetchManifestUsesDiscoveredPortBeforeConfiguredServerHttpPort() throws Exception {
        ManifestData manifest = manifest(List.of(
                new ManifestEntry(CategoryType.MOD, "mods/discovered.jar", "discovered.jar", 12L, "abc", true, true, "")
        ));
        HttpServer discovered = jsonServer(200, ManifestGenerator.toJson(manifest));
        discovered.start();

        try {
            int discoveredPort = discovered.getAddress().getPort();

            ManifestData fetched = ServerManifestHttpHandler.fetchManifest(
                    "127.0.0.1:25565",
                    discoveredPort,
                    2_000,
                    2_000,
                    discoveredPort + 1
            );

            assertEquals(List.of("MOD:mods/discovered.jar"),
                    fetched.getEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        } finally {
            discovered.stop(0);
        }
    }

    @Test
    void fetchManifestFallsBackToConfiguredServerHttpPortWhenDiscoveredPortFails() throws Exception {
        HttpServer configured = jsonServer(200, ManifestGenerator.toJson(manifest(List.of(
                new ManifestEntry(CategoryType.CONFIG, "client/fallback.txt", "fallback.txt", 4L, "def", true, false, "")
        ))));
        configured.start();

        try {
            int configuredPort = configured.getAddress().getPort();

            ManifestData fetched = ServerManifestHttpHandler.fetchManifest(
                    "127.0.0.1:25565",
                    configuredPort + 1000,
                    200,
                    200,
                    configuredPort
            );

            assertEquals(List.of("CONFIG:client/fallback.txt"),
                    fetched.getEntries().stream().map(ManifestEntry::getIdentityKey).toList());
        } finally {
            configured.stop(0);
        }
    }

    private static ManifestData manifest(List<ManifestEntry> entries) {
        ManifestData manifest = new ManifestData();
        manifest.setGeneratedAt(123456789L);
        manifest.setEntries(entries);
        return manifest;
    }

    private static HttpServer jsonServer(int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/manifest", exchange -> {
            exchange.sendResponseHeaders(statusCode, statusCode >= 200 && statusCode < 300 ? bytes.length : -1);
            if (statusCode >= 200 && statusCode < 300) {
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bytes);
                }
            } else {
                exchange.close();
            }
        });
        return server;
    }
}
