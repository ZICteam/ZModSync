package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandshakePolicyTest {
    private final HandshakeTracker handshakeTracker = new HandshakeTracker();

    @AfterEach
    void tearDown() {
        handshakeTracker.reset();
    }

    @Test
    void requiresClientHandshakeOnlyOnDedicatedServer() {
        assertTrue(HandshakePolicy.requiresClientHandshake(true));
        assertFalse(HandshakePolicy.requiresClientHandshake(false));
    }

    @Test
    void acknowledgedHandshakeShouldPreventLaterPendingRegistration() {
        UUID playerId = UUID.randomUUID();

        handshakeTracker.acknowledge(playerId);
        handshakeTracker.registerPending(playerId, 600);

        assertFalse(handshakeTracker.hasPending(playerId));
    }
}
