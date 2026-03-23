package com.modsync;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncLogBufferTest {
    @BeforeEach
    void setUp() {
        SyncLogBuffer.clear();
    }

    @AfterEach
    void tearDown() {
        SyncLogBuffer.clear();
    }

    @Test
    void snapshotPreservesAppendOrder() {
        SyncLogBuffer.append("line-1");
        SyncLogBuffer.append("line-2");

        assertEquals(List.of("line-1", "line-2"), SyncLogBuffer.snapshot());
    }

    @Test
    void bufferKeepsOnlyLastTwoHundredLines() {
        for (int i = 1; i <= 205; i++) {
            SyncLogBuffer.append("line-" + i);
        }

        List<String> snapshot = SyncLogBuffer.snapshot();
        assertEquals(200, snapshot.size());
        assertEquals("line-6", snapshot.get(0));
        assertEquals("line-205", snapshot.get(snapshot.size() - 1));
    }

    @Test
    void clearRemovesBufferedLines() {
        SyncLogBuffer.append("line-1");
        SyncLogBuffer.append("line-2");

        SyncLogBuffer.clear();

        assertEquals(List.of(), SyncLogBuffer.snapshot());
    }
}
