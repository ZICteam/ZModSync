package com.modsync;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadTaskTest {
    @Test
    void downloadTaskTracksEntryAndTargetFile() {
        ManifestEntry entry = new ManifestEntry(CategoryType.MOD, "mods/example.jar", "example.jar", 12L, "abc", true, true, "http://example/mods/example.jar");
        Path targetFile = Path.of("/tmp/modsync/example.jar");

        DownloadTask task = new DownloadTask(entry, targetFile);

        assertSame(entry, task.getEntry());
        assertEquals(targetFile, task.getTargetFile());
        assertEquals(0, task.getDownloadedBytes());
        assertEquals(0, task.getAttempts());
        assertFalse(task.isCompleted());
    }

    @Test
    void downloadTaskAccumulatesProgressAttemptsAndCompletionState() {
        ManifestEntry entry = new ManifestEntry(CategoryType.CONFIG, "client/options.txt", "options.txt", 4L, "def", true, false, "http://example/config/options.txt");
        DownloadTask task = new DownloadTask(entry, Path.of("/tmp/modsync/options.txt"));

        task.addDownloadedBytes(10);
        task.addDownloadedBytes(15);
        task.incrementAttempts();
        task.incrementAttempts();
        task.markCompleted();

        assertEquals(25, task.getDownloadedBytes());
        assertEquals(2, task.getAttempts());
        assertTrue(task.isCompleted());
    }
}
