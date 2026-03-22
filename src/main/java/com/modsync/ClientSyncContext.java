package com.modsync;

public final class ClientSyncContext {
    private static volatile String currentServerId = "";

    private ClientSyncContext() {
    }

    public static void setCurrentServerId(String serverId) {
        currentServerId = serverId == null ? "" : serverId.trim();
    }

    public static String getCurrentServerId() {
        return currentServerId;
    }

    public static void clear() {
        currentServerId = "";
    }
}
