package com.modsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpPathCodecTest {
    @Test
    void encodeRelativePathEscapesSpacesPlusAndPercentPerSegment() {
        assertEquals(
                "mods/My%20Cool%2BMod%20100%25.jar",
                HttpPathCodec.encodeRelativePath("mods/My Cool+Mod 100%.jar")
        );
    }

    @Test
    void decodeRelativePathRestoresNestedSegmentsWithSpacesAndPlus() {
        assertEquals(
                "client/My Cool+Pack/options 100%.txt",
                HttpPathCodec.decodeRelativePath("client/My%20Cool%2BPack/options%20100%25.txt")
        );
    }

    @Test
    void decodeRelativePathRejectsEncodedTraversalSeparators() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpPathCodec.decodeRelativePath("mods/%2Fsecret.jar"));
    }
}
