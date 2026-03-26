package com.modsync;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class HandshakeTracker {
    private final Map<UUID, Integer> pendingHandshakes = new ConcurrentHashMap<>();
    private final Set<UUID> acknowledgedHandshakes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> awaitingAdminDecision = ConcurrentHashMap.newKeySet();

    void acknowledge(UUID playerId) {
        if (playerId == null) {
            return;
        }
        acknowledgedHandshakes.add(playerId);
        pendingHandshakes.remove(playerId);
        awaitingAdminDecision.remove(playerId);
    }

    void registerPending(UUID playerId, int timeoutTicks) {
        if (playerId == null || acknowledgedHandshakes.contains(playerId)) {
            return;
        }
        awaitingAdminDecision.remove(playerId);
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
        awaitingAdminDecision.remove(playerId);
    }

    void markAwaitingAdminDecision(UUID playerId) {
        if (playerId == null) {
            return;
        }
        pendingHandshakes.remove(playerId);
        if (!acknowledgedHandshakes.contains(playerId)) {
            awaitingAdminDecision.add(playerId);
        }
    }

    boolean isAwaitingAdminDecision(UUID playerId) {
        return playerId != null && awaitingAdminDecision.contains(playerId);
    }

    Set<UUID> awaitingAdminDecisionView() {
        return awaitingAdminDecision;
    }

    Map<UUID, Integer> pendingView() {
        return pendingHandshakes;
    }

    void reset() {
        pendingHandshakes.clear();
        acknowledgedHandshakes.clear();
        awaitingAdminDecision.clear();
    }
}
