package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncComparatorTest {
    @Test
    void compareRecognizesMatchingEntriesIgnoringHashCase() {
        ManifestEntry server = entry(CategoryType.MOD, "mods/a.jar", 10L, "ABCDEF");
        ManifestEntry client = entry(CategoryType.MOD, "mods/a.jar", 10L, "abcdef");

        assertEquals(SyncComparator.ComparisonResult.MATCHES, SyncComparator.compare(server, client));
    }

    @Test
    void compareMarksMissingClientEntry() {
        ManifestEntry server = entry(CategoryType.CONFIG, "client/options.txt", 30L, "ccc");

        assertEquals(SyncComparator.ComparisonResult.MISSING, SyncComparator.compare(server, null));
    }

    @Test
    void compareMarksSizeChangeBeforeHashDifference() {
        ManifestEntry server = entry(CategoryType.CONFIG, "client/options.txt", 31L, "ccc-new");
        ManifestEntry client = entry(CategoryType.CONFIG, "client/options.txt", 30L, "ccc-old");

        assertEquals(SyncComparator.ComparisonResult.SIZE_CHANGED, SyncComparator.compare(server, client));
    }

    @Test
    void compareMarksHashChangeWhenSizeMatches() {
        ManifestEntry server = entry(CategoryType.RESOURCEPACK, "packs/ui.zip", 40L, "aaa");
        ManifestEntry client = entry(CategoryType.RESOURCEPACK, "packs/ui.zip", 40L, "bbb");

        assertEquals(SyncComparator.ComparisonResult.HASH_CHANGED, SyncComparator.compare(server, client));
    }

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

    @Test
    void findMissingOrOutdatedDoesNotMixEntriesFromDifferentCategories() {
        ManifestEntry localConfig = entry(CategoryType.CONFIG, "shared/file.dat", 10L, "aaa");

        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(
                entry(CategoryType.MOD, "shared/file.dat", 10L, "aaa"),
                entry(CategoryType.CONFIG, "shared/file.dat", 10L, "aaa")
        ));

        List<ManifestEntry> result = SyncComparator.findMissingOrOutdated(List.of(localConfig), manifest);

        assertEquals(List.of("MOD:shared/file.dat"), result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void findMissingOrOutdatedIgnoresMalformedLocalEntries() {
        ManifestEntry validServer = entry(CategoryType.MOD, "mods/a.jar", 10L, "aaa");
        ManifestEntry malformedLocal = entry(CategoryType.MOD, "mods/bad.jar", 10L, null);

        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(validServer));

        List<ManifestEntry> result = SyncComparator.findMissingOrOutdated(List.of(malformedLocal), manifest);

        assertEquals(List.of("MOD:mods/a.jar"), result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void findMissingOrOutdatedIgnoresMalformedServerEntries() {
        ManifestData manifest = new ManifestData();
        manifest.setEntries(List.of(
                entry(CategoryType.MOD, "mods/a.jar", 10L, "aaa"),
                entry(CategoryType.MOD, "mods/bad.jar", 10L, ""),
                entry(null, "mods/no-category.jar", 10L, "bbb")
        ));

        List<ManifestEntry> result = SyncComparator.findMissingOrOutdated(List.of(), manifest);

        assertEquals(List.of("MOD:mods/a.jar"), result.stream().map(ManifestEntry::getIdentityKey).toList());
    }

    @Test
    void compareTreatsMalformedServerEntryAsNoDownload() {
        ManifestEntry client = entry(CategoryType.MOD, "mods/a.jar", 10L, "aaa");
        ManifestEntry malformedServer = entry(CategoryType.MOD, "mods/a.jar", 10L, null);

        assertEquals(SyncComparator.ComparisonResult.MATCHES, SyncComparator.compare(malformedServer, client));
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
