package com.modsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotdMetadataCodecTest {
    @Test
    void appendAndExtractRoundTripHttpPort() {
        String encoded = MotdMetadataCodec.appendHiddenHttpPort("ModSync server", 8080);

        assertEquals(8080, MotdMetadataCodec.extractHttpPort(encoded));
        assertEquals("ModSync server", MotdMetadataCodec.stripHiddenMetadata(encoded));
    }

    @Test
    void appendReplacesPreviousHiddenMetadata() {
        String first = MotdMetadataCodec.appendHiddenHttpPort("Visible text", 8080);
        String replaced = MotdMetadataCodec.appendHiddenHttpPort(first, 9090);

        assertEquals(9090, MotdMetadataCodec.extractHttpPort(replaced));
        assertEquals("Visible text", MotdMetadataCodec.stripHiddenMetadata(replaced));
    }

    @Test
    void extractReturnsMissingForTextWithoutMetadata() {
        assertEquals(-1, MotdMetadataCodec.extractHttpPort("Plain MOTD"));
    }

    @Test
    void stripHiddenMetadataKeepsVisiblePrefixWhenTailIsBroken() {
        String broken = MotdMetadataCodec.appendHiddenHttpPort("Visible", 8080);
        broken = broken.substring(0, broken.length() - 1);

        assertEquals("Visible", MotdMetadataCodec.stripHiddenMetadata(broken));
        assertEquals(-1, MotdMetadataCodec.extractHttpPort(broken));
    }

    @Test
    void appendProducesHiddenSuffixAfterVisibleText() {
        String encoded = MotdMetadataCodec.appendHiddenHttpPort("Visible", 8080);

        assertTrue(encoded.startsWith("Visible"));
        assertTrue(encoded.length() > "Visible".length());
    }
}
