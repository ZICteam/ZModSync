package com.modsync;

final class SyncProgressStateResolver {
    private SyncProgressStateResolver() {
    }

    static SyncProgressVisualState resolve(boolean hasIssue, boolean downloadActive, boolean restartRequired, boolean hasLogs) {
        if (hasIssue) {
            return SyncProgressVisualState.FAILED;
        }
        if (downloadActive) {
            return SyncProgressVisualState.DOWNLOADING;
        }
        if (restartRequired) {
            return SyncProgressVisualState.RESTART_REQUIRED;
        }
        if (!hasLogs) {
            return SyncProgressVisualState.WAITING;
        }
        return SyncProgressVisualState.COMPLETE;
    }

    enum SyncProgressVisualState {
        FAILED(0xFF8A3A3A),
        DOWNLOADING(0xFF4B6EAF),
        RESTART_REQUIRED(0xFF8C6A2B),
        WAITING(0xFF2E7D4F),
        COMPLETE(0xFF2E7D4F);

        private final int accentColor;

        SyncProgressVisualState(int accentColor) {
            this.accentColor = accentColor;
        }

        int accentColor() {
            return accentColor;
        }

        boolean showsIssueSummary() {
            return this == FAILED;
        }

        boolean showsRestartSummary() {
            return this == RESTART_REQUIRED;
        }
    }
}
