package com.modsync;

public final class SyncIssueState {
    private static volatile String message = "";

    private SyncIssueState() {
    }

    public static void clear() {
        message = "";
    }

    public static void set(String value) {
        message = value == null ? "" : value.trim();
    }

    public static boolean hasIssue() {
        return !message.isBlank();
    }

    public static String getMessage() {
        return message;
    }
}
