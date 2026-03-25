package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManifestDataTest {
    @Test
    void getEntriesNormalizesNullInternalListToEmptyList() {
        ManifestData data = new ManifestData();
        data.setEntries(null);

        assertNotNull(data.getEntries());
        assertEquals(List.of(), data.getEntries());
    }

    @Test
    void setEntriesCopiesInputList() {
        ManifestEntry entry = new ManifestEntry(CategoryType.MOD, "mods/core.jar", "core.jar", 1L, "abc", true, true, "");
        List<ManifestEntry> source = new ArrayList<>(List.of(entry));

        ManifestData data = new ManifestData();
        data.setEntries(source);
        source.clear();

        assertEquals(List.of("MOD:mods/core.jar"),
                data.getEntries().stream().map(ManifestEntry::getIdentityKey).toList());
    }
}
