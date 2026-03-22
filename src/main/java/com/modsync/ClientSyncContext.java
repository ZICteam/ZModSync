package com.modsync;

public final class ClientSyncContext {
    private static volatile String currentServerId = "";
    private static volatile String preJoinReadyServerId = "";

    private ClientSyncContext() {
    }

    public static void setCurrentServerId(String serverId) {
        currentServerId = serverId == null ? "" : serverId.trim();
    }

    public static String getCurrentServerId() {
        return currentServerId;
    }

    public static void markPreJoinReady(String serverId) {
        preJoinReadyServerId = serverId == null ? "" : serverId.trim();
    }

    public static boolean consumePreJoinReady(String serverId) {
        String normalized = serverId == null ? "" : serverId.trim();
        if (!normalized.isEmpty() && normalized.equals(preJoinReadyServerId)) {
            preJoinReadyServerId = "";
            return true;
        }
        return false;
    }

    public static void clear() {
        currentServerId = "";
        preJoinReadyServerId = "";
    }
}
