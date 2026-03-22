package com.modsync;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
