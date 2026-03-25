package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientBootstrapTest {
    @AfterEach
    void tearDown() {
        ClientSyncContext.clear();
    }

    @Test
    void buildPostLoginSyncPlanSkipsHandshakeInSingleplayer() {
        AtomicInteger consumeCalls = new AtomicInteger();

        ClientBootstrap.PostLoginSyncPlan plan = ClientBootstrap.buildPostLoginSyncPlan(
                true,
                "play.example.net:25565",
                serverId -> {
                    consumeCalls.incrementAndGet();
                    return false;
                }
        );

        assertTrue(plan.skipHandshake());
        assertFalse(plan.acknowledgeOnly());
        assertFalse(plan.startHandshake());
        assertEquals("Skipping ModSync handshake in singleplayer/local session", plan.reason());
        assertEquals(0, consumeCalls.get());
    }

    @Test
    void buildPostLoginSyncPlanSkipsHandshakeWhenServerIdIsBlank() {
        AtomicInteger consumeCalls = new AtomicInteger();

        ClientBootstrap.PostLoginSyncPlan plan = ClientBootstrap.buildPostLoginSyncPlan(
                false,
                "   ",
                serverId -> {
                    consumeCalls.incrementAndGet();
                    return false;
                }
        );

        assertTrue(plan.skipHandshake());
        assertFalse(plan.acknowledgeOnly());
        assertFalse(plan.startHandshake());
        assertEquals("Skipping ModSync handshake in singleplayer/local session", plan.reason());
        assertEquals(0, consumeCalls.get());
    }

    @Test
    void buildPostLoginSyncPlanAcknowledgesServerAfterPreJoinReady() {
        ClientSyncContext.markPreJoinReady("  play.example.net:25565  ");

        ClientBootstrap.PostLoginSyncPlan plan = ClientBootstrap.buildPostLoginSyncPlan(
                false,
                "play.example.net:25565",
                ClientSyncContext::consumePreJoinReady
        );

        assertFalse(plan.skipHandshake());
        assertTrue(plan.acknowledgeOnly());
        assertFalse(plan.startHandshake());
        assertEquals("Sending ModSync handshake acknowledgement after pre-join sync", plan.reason());
        assertFalse(ClientSyncContext.consumePreJoinReady("play.example.net:25565"));
    }

    @Test
    void buildPostLoginSyncPlanStartsHandshakeForRegularMultiplayerLogin() {
        AtomicInteger consumeCalls = new AtomicInteger();

        ClientBootstrap.PostLoginSyncPlan plan = ClientBootstrap.buildPostLoginSyncPlan(
                false,
                "  play.example.net:25565  ",
                serverId -> {
                    consumeCalls.incrementAndGet();
                    assertEquals("play.example.net:25565", serverId);
                    return false;
                }
        );

        assertFalse(plan.skipHandshake());
        assertFalse(plan.acknowledgeOnly());
        assertTrue(plan.startHandshake());
        assertEquals("Client connection established, starting sync handshake", plan.reason());
        assertEquals(1, consumeCalls.get());
    }
}
