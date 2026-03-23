package com.modsync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    void sha256MatchesKnownValueForTextFile() throws Exception {
        Path file = tempDir.resolve("sample.txt");
        Files.writeString(file, "abc");

        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HashUtils.sha256(file)
        );
    }

    @Test
    void sha256MatchesKnownValueForEmptyFile() throws Exception {
        Path file = tempDir.resolve("empty.bin");
        Files.write(file, new byte[0]);

        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                HashUtils.sha256(file)
        );
    }

    @Test
    void sha256ChangesWhenFileContentsChange() throws Exception {
        Path file = tempDir.resolve("mutable.txt");
        Files.writeString(file, "first");
        String first = HashUtils.sha256(file);

        Files.writeString(file, "second");
        String second = HashUtils.sha256(file);

        assertNotEquals(first, second);
    }

    @Test
    void sha256UsesLowercaseSixtyFourCharacterHexEncoding() throws Exception {
        Path file = tempDir.resolve("hex.txt");
        Files.writeString(file, "hex-check");

        String hash = HashUtils.sha256(file);

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}
