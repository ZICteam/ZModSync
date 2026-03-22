package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncComparatorTest {
    @Test
    void findMissingOrOutdatedReturnsOnlyChangedEntries() {
        ManifestEntry currentMod = entry(CategoryType.MOD, "mods/a.jar", 10L, "aaa");
        ManifestEntry changedConfig = entry(CategoryType.CONFIG, "client/options.txt", 30L, "ccc");
        ManifestEntry missingShader = entry(CategoryType.SHADERPACK, "packs/visual.zip", 40L, "ddd");

        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(
                entry(CategoryType.MOD, "mods/a.jar", 10L, "aaa"),
                entry(CategoryType.CONFIG, "client/options.txt", 31L, "ccc-new"),
                missingShader
        ));

        List<ManifestEntry> result = SyncComparator.findMissingOrOutdated(List.of(currentMod, changedConfig), manifest);

        assertEquals(List.of(
                "CONFIG:client/options.txt",
                "SHADERPACK:packs/visual.zip"
        ), result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    private static ManifestEntry entry(CategoryType category, String relativePath, long fileSize, String sha256) {
        return new ManifestEntry(category, relativePath, PathName.fileName(relativePath), fileSize, sha256, true, false, "");
    }

    private static final class PathName {
        private static String fileName(String path) {
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        }
    }
}
