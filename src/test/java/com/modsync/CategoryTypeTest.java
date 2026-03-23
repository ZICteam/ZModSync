package com.modsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTypeTest {
    @Test
    void fromHttpSegmentResolvesKnownSegmentsCaseInsensitively() {
        assertEquals(CategoryType.MOD, CategoryType.fromHttpSegment("mod"));
        assertEquals(CategoryType.RESOURCEPACK, CategoryType.fromHttpSegment("RESOURCEPACK"));
        assertEquals(CategoryType.SHADERPACK, CategoryType.fromHttpSegment("shaderpack"));
        assertEquals(CategoryType.CONFIG, CategoryType.fromHttpSegment("Config"));
        assertEquals(CategoryType.OPTIONAL_CLIENT, CategoryType.fromHttpSegment("optional_client"));
    }

    @Test
    void fromHttpSegmentRejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> CategoryType.fromHttpSegment("unknown"));
    }

    @Test
    void httpSegmentMatchesLowerCaseEnumName() {
        for (CategoryType type : CategoryType.values()) {
            assertEquals(type.name().toLowerCase(), type.getHttpSegment());
        }
    }

    @Test
    void restartDefaultsMatchCurrentSyncDesign() {
        assertTrue(CategoryType.MOD.isDefaultRestartRequired());
        assertTrue(CategoryType.OPTIONAL_CLIENT.isDefaultRestartRequired());
        assertEquals(false, CategoryType.CONFIG.isDefaultRestartRequired());
        assertEquals(false, CategoryType.RESOURCEPACK.isDefaultRestartRequired());
        assertEquals(false, CategoryType.SHADERPACK.isDefaultRestartRequired());
    }
}
