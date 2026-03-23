package com.modsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ManifestEntryTest {
    @Test
    void identityKeyCombinesCategoryAndRelativePath() {
        ManifestEntry entry = new ManifestEntry(
                CategoryType.CONFIG,
                "client/options.txt",
                "options.txt",
                42L,
                "abc",
                true,
                false,
                ""
        );

        assertEquals("CONFIG:client/options.txt", entry.getIdentityKey());
    }

    @Test
    void copyReturnsIndependentObjectWithSameData() {
        ManifestEntry original = new ManifestEntry(
                CategoryType.MOD,
                "mods/core.jar",
                "core.jar",
                100L,
                "abc",
                true,
                true,
                "http://example/mods/core.jar"
        );

        ManifestEntry copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getIdentityKey(), copy.getIdentityKey());
        assertEquals(original.getFileName(), copy.getFileName());
        assertEquals(original.getFileSize(), copy.getFileSize());
        assertEquals(original.getSha256(), copy.getSha256());
        assertEquals(original.isRequired(), copy.isRequired());
        assertEquals(original.isRestartRequired(), copy.isRestartRequired());
        assertEquals(original.getDownloadUrl(), copy.getDownloadUrl());
    }

    @Test
    void copyDoesNotMutateOriginalWhenChanged() {
        ManifestEntry original = new ManifestEntry(
                CategoryType.RESOURCEPACK,
                "packs/ui.zip",
                "ui.zip",
                20L,
                "aaa",
                false,
                false,
                "http://example/packs/ui.zip"
        );

        ManifestEntry copy = original.copy();
        copy.setDownloadUrl("http://example/packs/ui-v2.zip");
        copy.setSha256("bbb");

        assertEquals("http://example/packs/ui.zip", original.getDownloadUrl());
        assertEquals("aaa", original.getSha256());
    }
}
