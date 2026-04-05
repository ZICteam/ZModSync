package com.modsync;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModrinthApiClientTest {
    @AfterEach
    void tearDown() {
        ModrinthApiClient.resetCacheForTests();
    }

    @Test
    void resolveDownloadUrlReturnsPrimaryMatchingSha1File() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/version_file/a9993e364706816aba3e25717850c26c9cd0d89d", exchange -> {
            byte[] body = """
                    {
                      "files": [
                        {
                          "url": "https://cdn.modrinth.com/data/example/non-primary.jar",
                          "primary": false,
                          "hashes": {
                            "sha1": "a9993e364706816aba3e25717850c26c9cd0d89d"
                          }
                        },
                        {
                          "url": "https://cdn.modrinth.com/data/example/primary.jar",
                          "primary": true,
                          "hashes": {
                            "sha1": "a9993e364706816aba3e25717850c26c9cd0d89d"
                          }
                        }
                      ]
                    }
                    """.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            ManifestEntry entry = new ManifestEntry(
                    CategoryType.MOD,
                    "mods/example.jar",
                    "example.jar",
                    1L,
                    "sha256",
                    true,
                    true,
                    "http://server/files/mod/example.jar",
                    "a9993e364706816aba3e25717850c26c9cd0d89d"
            );

            assertEquals(
                    "https://cdn.modrinth.com/data/example/primary.jar",
                    ModrinthApiClient.resolveDownloadUrl(
                            entry,
                            "http://127.0.0.1:" + server.getAddress().getPort(),
                            1_000,
                            1_000
                    )
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resolveDownloadUrlReturnsNullWhenSha1DoesNotMatchAnyReturnedFile() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/version_file/nomatch", exchange -> {
            byte[] body = """
                    {
                      "files": [
                        {
                          "url": "https://cdn.modrinth.com/data/example/file.jar",
                          "primary": true,
                          "hashes": {
                            "sha1": "different"
                          }
                        }
                      ]
                    }
                    """.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        server.start();

        try {
            ManifestEntry entry = new ManifestEntry(
                    CategoryType.MOD,
                    "mods/example.jar",
                    "example.jar",
                    1L,
                    "sha256",
                    true,
                    true,
                    "http://server/files/mod/example.jar",
                    "nomatch"
            );

            assertNull(ModrinthApiClient.resolveDownloadUrl(
                    entry,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    1_000,
                    1_000
            ));
        } finally {
            server.stop(0);
        }
    }
}
