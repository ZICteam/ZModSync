package com.modsync;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilsTest {
    @Test
    void resolveSafeChildAllowsNormalRelativePath() {
        Path root = Path.of("/tmp/modsync-root");
        Path resolved = FileUtils.resolveSafeChild(root, "mods/example.jar");

        assertEquals(root.resolve("mods/example.jar").normalize(), resolved);
    }

    @Test
    void resolveSafeChildBlocksPathTraversal() {
        Path root = Path.of("/tmp/modsync-root");

        assertThrows(IllegalArgumentException.class, () -> FileUtils.resolveSafeChild(root, "../outside.jar"));
    }

    @Test
    void sanitizeServerIdReplacesUnsafeCharacters() {
        assertEquals("mc.example.com_25565", FileUtils.sanitizeServerId("mc.example.com:25565"));
    }

    @Test
    void toRelativeUnixPathNormalizesSeparatorsToForwardSlashes() {
        Path root = Path.of("/tmp/modsync-root");
        Path file = Path.of("/tmp/modsync-root/config/subdir/options.txt");

        assertEquals("config/subdir/options.txt", FileUtils.toRelativeUnixPath(root, file));
    }

    @Test
    void isSkippedFileMatchesExtensionsCaseInsensitively() {
        assertTrue(FileUtils.isSkippedFile(Path.of("/tmp/latest.LOG"), Set.of(".log", ".tmp")));
    }

    @Test
    void isSkippedFileLeavesUnlistedExtensionsAlone() {
        assertFalse(FileUtils.isSkippedFile(Path.of("/tmp/mod.jar"), Set.of(".log", ".tmp")));
    }

    @Test
    void isProtectedModFileNameMatchesCaseInsensitively() {
        assertTrue(FileUtils.isProtectedModFileName("ModSync-1.0.31.jar"));
    }

    @Test
    void isProtectedModPathChecksOnlyFileName() {
        assertTrue(FileUtils.isProtectedModPath(Path.of("/mods/clientside/MODSYNC-helper.jar")));
    }

    @Test
    void isProtectedModFileNameRejectsBlankOrUnrelatedNames() {
        assertFalse(FileUtils.isProtectedModFileName(""));
        assertFalse(FileUtils.isProtectedModFileName("examplemod-1.0.0.jar"));
    }
}
