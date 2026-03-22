package com.modsync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RestartState {
    private static final AtomicBoolean RESTART_REQUIRED = new AtomicBoolean(false);
    private static final AtomicBoolean PROMPT_PENDING = new AtomicBoolean(false);
    private static final Object CHANGE_LOCK = new Object();
    private static final Map<String, RestartChange> CHANGES = new LinkedHashMap<>();

    private RestartState() {
    }

    public static void markRestartRequired() {
        RESTART_REQUIRED.set(true);
        PROMPT_PENDING.set(true);
    }

    public static boolean isRestartRequired() {
        return RESTART_REQUIRED.get();
    }

    public static boolean consumePromptPending() {
        return PROMPT_PENDING.compareAndSet(true, false);
    }

    public static void recordDownloaded(ManifestEntry entry) {
        recordChange(ChangeType.DOWNLOADED, entry);
    }

    public static void recordDeleted(ManifestEntry entry) {
        recordChange(ChangeType.DELETED, entry);
    }

    public static List<RestartChange> snapshotChanges() {
        synchronized (CHANGE_LOCK) {
            return new ArrayList<>(CHANGES.values());
        }
    }

    private static void recordChange(ChangeType type, ManifestEntry entry) {
        if (entry == null) {
            return;
        }
        synchronized (CHANGE_LOCK) {
            CHANGES.put(type.name() + ":" + entry.getIdentityKey(),
                    new RestartChange(type, entry.getCategory(), entry.getRelativePath()));
        }
    }

    public enum ChangeType {
        DOWNLOADED,
        DELETED
    }

    public record RestartChange(ChangeType type, CategoryType category, String relativePath) {
    }
}
