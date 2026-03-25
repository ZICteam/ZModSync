package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientSyncContextTest {
    @AfterEach
    void tearDown() {
        ClientSyncContext.clear();
    }

    @Test
    void setCurrentServerIdTrimsAndStoresValue() {
        ClientSyncContext.setCurrentServerId("  play.example.net:25565  ");

        assertEquals("play.example.net:25565", ClientSyncContext.getCurrentServerId());
    }

    @Test
    void setCurrentServerHttpPortStoresOnlyPositiveValues() {
        ClientSyncContext.setCurrentServerHttpPort(26590);
        assertEquals(26590, ClientSyncContext.getCurrentServerHttpPort());

        ClientSyncContext.setCurrentServerHttpPort(-1);
        assertEquals(-1, ClientSyncContext.getCurrentServerHttpPort());
    }

    @Test
    void markAndConsumePreJoinReadyIsSingleUseForMatchingServer() {
        ClientSyncContext.markPreJoinReady("  play.example.net:25565  ");

        assertTrue(ClientSyncContext.consumePreJoinReady("play.example.net:25565"));
        assertFalse(ClientSyncContext.consumePreJoinReady("play.example.net:25565"));
    }

    @Test
    void consumePreJoinReadyRejectsDifferentOrBlankServerIds() {
        ClientSyncContext.markPreJoinReady("play.example.net:25565");

        assertFalse(ClientSyncContext.consumePreJoinReady("other.example.net:25565"));
        assertFalse(ClientSyncContext.consumePreJoinReady(""));
        assertFalse(ClientSyncContext.consumePreJoinReady(null));
    }

    @Test
    void clearResetsBothCurrentAndPreJoinState() {
        ClientSyncContext.setCurrentServerId("play.example.net:25565");
        ClientSyncContext.markPreJoinReady("play.example.net:25565");
        ClientSyncContext.setCurrentServerHttpPort(26590);

        ClientSyncContext.clear();

        assertEquals("", ClientSyncContext.getCurrentServerId());
        assertEquals(-1, ClientSyncContext.getCurrentServerHttpPort());
        assertFalse(ClientSyncContext.consumePreJoinReady("play.example.net:25565"));
    }
}
