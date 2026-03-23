package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncIssueStateTest {
    @AfterEach
    void tearDown() {
        SyncIssueState.clear();
    }

    @Test
    void setTrimsAndStoresIssueMessage() {
        SyncIssueState.set("  download failed  ");

        assertTrue(SyncIssueState.hasIssue());
        assertEquals("download failed", SyncIssueState.getMessage());
    }

    @Test
    void setNullClearsIssueState() {
        SyncIssueState.set("temporary issue");
        SyncIssueState.set(null);

        assertFalse(SyncIssueState.hasIssue());
        assertEquals("", SyncIssueState.getMessage());
    }

    @Test
    void clearRemovesStoredIssue() {
        SyncIssueState.set("network timeout");

        SyncIssueState.clear();

        assertFalse(SyncIssueState.hasIssue());
        assertEquals("", SyncIssueState.getMessage());
    }
}
