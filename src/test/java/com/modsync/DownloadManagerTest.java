package com.modsync;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadManagerTest {
    @Test
    void describeExceptionMapsTimeoutToFriendlyMessage() {
        assertEquals("network timeout", DownloadManager.describeException(new SocketTimeoutException("Read timed out")));
    }

    @Test
    void describeExceptionFallsBackToExceptionMessage() {
        assertEquals("HTTP 404 while downloading mods/a.jar",
                DownloadManager.describeException(new IOException("HTTP 404 while downloading mods/a.jar")));
    }

    @Test
    void describeExceptionHandlesNull() {
        assertEquals("unknown error", DownloadManager.describeException(null));
    }

    @Test
    void buildSingleDownloadFailureMessageUsesRelativePath() {
        assertEquals(
                "Failed to download mods/example.jar: network timeout",
                DownloadManager.buildSingleDownloadFailureMessage("mods/example.jar", new SocketTimeoutException("timeout"))
        );
    }

    @Test
    void buildDownloadBatchFailureMessageIncludesCount() {
        assertEquals(
                "Download step failed for 3 file(s). Check the log panel for details.",
                DownloadManager.buildDownloadBatchFailureMessage(3)
        );
    }
}
