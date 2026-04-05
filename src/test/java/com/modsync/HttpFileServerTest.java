package com.modsync;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpFileServerTest {
    @Test
    void resolveApprovedEntryMatchesEncodedFileNameWithSpacesAndSymbols() {
        ManifestEntry approved = new ManifestEntry(
                CategoryType.MOD,
                "mods/My Cool+Mod 100%.jar",
                "My Cool+Mod 100%.jar",
                12L,
                "abc",
                true,
                true,
                ""
        );

        ManifestEntry resolved = HttpFileServer.resolveApprovedEntry(
                "/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                Map.of(approved.getIdentityKey(), approved)
        );

        assertEquals(approved.getIdentityKey(), resolved.getIdentityKey());
    }

    @Test
    void resolveApprovedEntryReturnsNullWhenEntryWasNotApproved() {
        ManifestEntry resolved = HttpFileServer.resolveApprovedEntry(
                "/files/mod/mods/Missing%20Mod.jar",
                Map.of()
        );

        assertNull(resolved);
    }

    @Test
    void resolveApprovedEntryRejectsEncodedTraversalPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpFileServer.resolveApprovedEntry(
                        "/files/mod/mods/%2Fsecret.jar",
                        Map.of()
                ));
    }

    @Test
    void resolveApprovedEntryRejectsMalformedFilePathWithoutCategorySeparator() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpFileServer.resolveApprovedEntry("/files/mods-only", Map.of()));
    }

    @Test
    void stopShutsDownHttpExecutor() throws Exception {
        HttpFileServer instance = HttpFileServer.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();

        Field serverField = HttpFileServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Field executorField = HttpFileServer.class.getDeclaredField("executorService");
        executorField.setAccessible(true);

        serverField.set(instance, server);
        executorField.set(instance, executor);

        instance.stop();

        assertTrue(executor.isShutdown());
        assertNull(serverField.get(instance));
        assertNull(executorField.get(instance));
    }
}
