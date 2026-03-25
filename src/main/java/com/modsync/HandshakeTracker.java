package com.modsync;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HandshakeTracker {
    private final Map<UUID, Integer> pendingHandshakes = new ConcurrentHashMap<>();
    private final Set<UUID> acknowledgedHandshakes = ConcurrentHashMap.newKeySet();

    void acknowledge(UUID playerId) {
        if (playerId == null) {
            return;
        }
        acknowledgedHandshakes.add(playerId);
        pendingHandshakes.remove(playerId);
    }

    void registerPending(UUID playerId, int timeoutTicks) {
        if (playerId == null || acknowledgedHandshakes.contains(playerId)) {
            return;
        }
        pendingHandshakes.put(playerId, timeoutTicks);
    }

    boolean hasPending(UUID playerId) {
        return playerId != null && pendingHandshakes.containsKey(playerId);
    }

    void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }
        pendingHandshakes.remove(playerId);
        acknowledgedHandshakes.remove(playerId);
    }

    Map<UUID, Integer> pendingView() {
        return pendingHandshakes;
    }

    void reset() {
        pendingHandshakes.clear();
        acknowledgedHandshakes.clear();
    }
}
