package com.modsync;

import java.nio.file.Path;

public final class SelfUpdateCoordinator {
    private SelfUpdateCoordinator() {
    }

    public static void initShutdownHook() {
        // Self-update launchers were removed to satisfy distribution security rules.
    }

    public static SelfUpdatePlan planForEntry(ManifestEntry entry, Path defaultTarget) {
        return new SelfUpdatePlan(false, defaultTarget, null, null, null);
    }

    static SelfUpdatePlan planForEntry(ManifestEntry entry, Path defaultTarget, Path currentJarPath) {
        return new SelfUpdatePlan(false, defaultTarget, null, null, null);
    }

    public static void registerDownloadedUpdate(SelfUpdatePlan plan) {
        if (plan != null && plan.active() && plan.targetJarPath() != null) {
            LoggerUtils.warn("Ignoring staged self-update request for " + plan.targetJarPath().getFileName()
                    + " because self-update launchers are disabled in distributed builds.");
        }
    }

    static boolean isSelfUpdateEntry(ManifestEntry entry) {
        return entry != null
                && entry.getCategory() == CategoryType.MOD
                && FileUtils.isProtectedModFileName(entry.getFileName());
    }

    record SelfUpdatePlan(boolean active,
                          Path downloadTargetPath,
                          Path currentJarPath,
                          Path targetJarPath,
                          Path stagedJarPath) {
    }
}
