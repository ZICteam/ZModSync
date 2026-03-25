package com.modsync;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfUpdateCoordinatorTest {
    @Test
    void planForEntryStagesProtectedModsyncJarForPostExitReplacement() {
        ManifestEntry entry = new ManifestEntry(
                CategoryType.MOD,
                "modsync-1.0.88.jar",
                "modsync-1.0.88.jar",
                1L,
                "abc",
                true,
                true,
                "https://example.invalid/modsync-1.0.88.jar"
        );
        Path defaultTarget = Path.of("/game/mods/modsync-1.0.88.jar");
        Path currentJar = Path.of("/game/mods/modsync-1.0.87.jar");

        SelfUpdateCoordinator.SelfUpdatePlan plan =
                SelfUpdateCoordinator.planForEntry(entry, defaultTarget, currentJar);

        assertTrue(plan.active());
        assertEquals(Path.of("/game/.modsync/self-update/modsync-1.0.88.jar.pending"), plan.downloadTargetPath());
        assertEquals(currentJar, plan.currentJarPath());
        assertEquals(defaultTarget, plan.targetJarPath());
        assertEquals(Path.of("/game/.modsync/self-update/modsync-1.0.88.jar.pending"), plan.stagedJarPath());
    }

    @Test
    void planForEntryLeavesRegularModsOnNormalTargetPath() {
        ManifestEntry entry = new ManifestEntry(
                CategoryType.MOD,
                "examplemod-1.0.0.jar",
                "examplemod-1.0.0.jar",
                1L,
                "abc",
                true,
                true,
                "https://example.invalid/examplemod-1.0.0.jar"
        );
        Path defaultTarget = Path.of("/game/mods/examplemod-1.0.0.jar");

        SelfUpdateCoordinator.SelfUpdatePlan plan =
                SelfUpdateCoordinator.planForEntry(entry, defaultTarget, Path.of("/game/mods/modsync-1.0.87.jar"));

        assertFalse(plan.active());
        assertEquals(defaultTarget, plan.downloadTargetPath());
    }

    @Test
    void isSelfUpdateEntryMatchesOnlyProtectedModsyncModEntries() {
        assertTrue(SelfUpdateCoordinator.isSelfUpdateEntry(
                new ManifestEntry(CategoryType.MOD, "modsync-1.0.88.jar", "modsync-1.0.88.jar", 1L, "abc", true, true, "")
        ));
        assertFalse(SelfUpdateCoordinator.isSelfUpdateEntry(
                new ManifestEntry(CategoryType.MOD, "othermod.jar", "othermod.jar", 1L, "abc", true, true, "")
        ));
        assertFalse(SelfUpdateCoordinator.isSelfUpdateEntry(
                new ManifestEntry(CategoryType.CONFIG, "modsync.txt", "modsync.txt", 1L, "abc", true, false, "")
        ));
    }
}
