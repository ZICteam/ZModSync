package com.modsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncProgressStateResolverTest {
    @Test
    void issueStateHasHighestPriority() {
        SyncProgressStateResolver.SyncProgressVisualState state =
                SyncProgressStateResolver.resolve(true, true, true, true);

        assertEquals(SyncProgressStateResolver.SyncProgressVisualState.FAILED, state);
        assertTrue(state.showsIssueSummary());
        assertFalse(state.showsRestartSummary());
    }

    @Test
    void activeDownloadWinsBeforeRestart() {
        SyncProgressStateResolver.SyncProgressVisualState state =
                SyncProgressStateResolver.resolve(false, true, true, true);

        assertEquals(SyncProgressStateResolver.SyncProgressVisualState.DOWNLOADING, state);
    }

    @Test
    void restartWinsBeforeCompletedState() {
        SyncProgressStateResolver.SyncProgressVisualState state =
                SyncProgressStateResolver.resolve(false, false, true, true);

        assertEquals(SyncProgressStateResolver.SyncProgressVisualState.RESTART_REQUIRED, state);
        assertTrue(state.showsRestartSummary());
        assertFalse(state.showsIssueSummary());
    }

    @Test
    void noLogsMeansWaiting() {
        SyncProgressStateResolver.SyncProgressVisualState state =
                SyncProgressStateResolver.resolve(false, false, false, false);

        assertEquals(SyncProgressStateResolver.SyncProgressVisualState.WAITING, state);
    }

    @Test
    void existingLogsWithoutActiveWorkMeansComplete() {
        SyncProgressStateResolver.SyncProgressVisualState state =
                SyncProgressStateResolver.resolve(false, false, false, true);

        assertEquals(SyncProgressStateResolver.SyncProgressVisualState.COMPLETE, state);
    }
}
