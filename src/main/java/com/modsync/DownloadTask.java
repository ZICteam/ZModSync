package com.modsync;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask {
    private final ManifestEntry entry;
    private final Path targetFile;
    private final AtomicLong downloadedBytes = new AtomicLong();
    private volatile int attempts;
    private volatile boolean completed;

    public DownloadTask(ManifestEntry entry, Path targetFile) {
        this.entry = entry;
        this.targetFile = targetFile;
    }

    public ManifestEntry getEntry() {
        return entry;
    }

    public Path getTargetFile() {
        return targetFile;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void addDownloadedBytes(long amount) {
        downloadedBytes.addAndGet(amount);
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        attempts++;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }
}
