package com.modsync;

final class HandshakePolicy {
    private HandshakePolicy() {
    }

    static boolean requiresClientHandshake(boolean dedicatedServer) {
        return dedicatedServer;
    }
}
