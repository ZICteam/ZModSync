package com.modsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class DownloadManager {
    private static final DownloadManager INSTANCE = new DownloadManager();

    private volatile ExecutorService executorService;
    private volatile int totalTasks;
    private final AtomicInteger completedTasks = new AtomicInteger();
    private volatile boolean active;

    private DownloadManager() {
    }

    public static DownloadManager getInstance() {
        return INSTANCE;
    }

    public synchronized void startDownloads(List<ManifestEntry> entries) {
        startDownloads(entries, null);
    }

    public synchronized void startDownloads(List<ManifestEntry> entries, Runnable completionAction) {
        if (entries.isEmpty()) {
            LoggerUtils.info("No downloads required");
            if (completionAction != null) {
                completionAction.run();
            }
            return;
        }

        shutdownExecutor();
        this.executorService = Executors.newFixedThreadPool(ConfigManager.downloadThreads());
        this.totalTasks = entries.size();
        this.completedTasks.set(0);
        this.active = true;

        AtomicBoolean restartRequired = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger failedDownloads = new AtomicInteger();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (ManifestEntry entry : entries) {
            LoggerUtils.info("Queued: " + entry.getCategory().name() + " / " + entry.getRelativePath());
            DownloadTask task = new DownloadTask(entry, FileUtils.resolveSafeChild(
                    FileUtils.resolveClientTargetRoot(entry.getCategory(), ClientSyncContext.getCurrentServerId()), entry.getRelativePath()));

            futures.add(CompletableFuture.runAsync(() -> {
                if (downloadWithRetry(task)) {
                    if (entry.isRestartRequired()) {
                        restartRequired.set(true);
                        RestartState.recordDownloaded(entry);
                    }
                    completedTasks.incrementAndGet();
                } else {
                    failed.set(true);
                    failedDownloads.incrementAndGet();
                }
            }, executorService));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, throwable) -> {
                    active = false;
                    shutdownExecutor();
                    if (throwable != null) {
                        LoggerUtils.error("Download queue completed with failures", throwable);
                    } else {
                        LoggerUtils.info("Download queue complete");
                    }
                    if (failed.get()) {
                        SyncIssueState.set(buildDownloadBatchFailureMessage(failedDownloads.get()));
                    }
                    if (restartRequired.get()) {
                        RestartState.markRestartRequired();
                        LoggerUtils.info("Restart requested after synchronized downloads");
                    }
                    if (throwable == null && !failed.get() && completionAction != null) {
                        completionAction.run();
                    }
                });
    }

    public boolean isActive() {
        return active;
    }

    public int getCompletedTasks() {
        return completedTasks.get();
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    private boolean downloadWithRetry(DownloadTask task) {
        Exception lastFailure = null;
        for (int i = 0; i <= ConfigManager.retryCount(); i++) {
            task.incrementAttempts();
            try {
                download(task);
                task.markCompleted();
                LoggerUtils.info("Downloaded " + task.getEntry().getRelativePath());
                return true;
            } catch (Exception exception) {
                lastFailure = exception;
                LoggerUtils.warn("Download attempt " + task.getAttempts() + " failed for "
                        + task.getEntry().getRelativePath() + ": " + describeException(exception));
            }
        }
        SyncIssueState.set(buildSingleDownloadFailureMessage(task.getEntry().getRelativePath(), lastFailure));
        return false;
    }

    private void download(DownloadTask task) throws IOException {
        ManifestEntry entry = task.getEntry();
        Path targetFile = task.getTargetFile();
        FileUtils.ensureParentExists(targetFile);
        LoggerUtils.info("Downloading: " + entry.getDownloadUrl());

        Path downloadFile = ConfigManager.useTempFiles()
                ? targetFile.resolveSibling(targetFile.getFileName() + ".modsync.tmp")
                : targetFile;

        HttpURLConnection connection = (HttpURLConnection) new URL(entry.getDownloadUrl()).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HTTP " + responseCode + " while downloading " + entry.getRelativePath());
        }

        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(downloadFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
                task.addDownloadedBytes(read);
            }
        }

        if (ConfigManager.verifyHashAfterDownload()) {
            String hash = HashUtils.sha256(downloadFile);
            if (!hash.equalsIgnoreCase(entry.getSha256())) {
                if (ConfigManager.deleteInvalidFiles()) {
                    Files.deleteIfExists(downloadFile);
                }
                LoggerUtils.warn("Hash verification failed for " + entry.getRelativePath());
                throw new IOException("Invalid SHA-256 for " + entry.getRelativePath());
            }
        }

        if (ConfigManager.useTempFiles()) {
            try {
                Files.move(downloadFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(downloadFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private synchronized void shutdownExecutor() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    static String buildDownloadBatchFailureMessage(int failedDownloads) {
        return "Download step failed for " + failedDownloads + " file(s). Check the log panel for details.";
    }

    static String buildSingleDownloadFailureMessage(String relativePath, Exception exception) {
        return "Failed to download " + relativePath + ": " + describeException(exception);
    }

    static String describeException(Exception exception) {
        if (exception == null) {
            return "unknown error";
        }
        if (exception instanceof SocketTimeoutException) {
            return "network timeout";
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
