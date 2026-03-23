package com.modsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ManifestGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ManifestGenerator() {
    }

    public static ManifestData generateManifest() {
        Set<String> skipped = ConfigManager.skipFileExtensions();
        Map<CategoryType, Path> roots = new EnumMap<>(CategoryType.class);

        for (CategoryType category : CategoryType.values()) {
            if (!ConfigManager.isCategoryEnabled(category)) {
                continue;
            }
            roots.put(category, FileUtils.resolveServerSourceRoot(category));
        }

        ManifestData data = generateManifest(roots, skipped, FileUtils.configDir().resolve("modsync-manifest.json").normalize());
        LoggerUtils.info("Generated manifest with " + data.getEntries().size() + " entries");
        return data;
    }

    static ManifestData generateManifest(Map<CategoryType, Path> roots, Set<String> skippedExtensions, Path manifestCopyPath) {
        ManifestData data = new ManifestData();
        data.setGeneratedAt(System.currentTimeMillis());

        List<ManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<CategoryType, Path> rootEntry : roots.entrySet()) {
            CategoryType category = rootEntry.getKey();
            Path root = rootEntry.getValue();
            if (!Files.exists(root)) {
                continue;
            }

            try (FileHashCache.ScopeSession hashCache = FileHashCache.openServerScope(category)) {
                Files.walk(root)
                        .filter(Files::isRegularFile)
                        .filter(path -> shouldIncludeInManifest(category, path))
                        .filter(path -> !FileUtils.isSkippedFile(path, skippedExtensions))
                        .forEach(path -> entries.add(createEntry(category, root, path, hashCache)));
            } catch (IOException exception) {
                LoggerUtils.error("Failed generating manifest for " + category, exception);
            }
        }

        data.setEntries(entries);
        writeManifestCopy(data, manifestCopyPath);
        return data;
    }

    public static String toJson(ManifestData data) {
        return GSON.toJson(data);
    }

    public static ManifestData fromJson(String json) {
        return GSON.fromJson(json, ManifestData.class);
    }

    public static List<ManifestEntry> entriesFromJson(String json) {
        ManifestEntry[] entries = GSON.fromJson(json, ManifestEntry[].class);
        List<ManifestEntry> list = new ArrayList<>();
        if (entries != null) {
            for (ManifestEntry entry : entries) {
                list.add(entry);
            }
        }
        return list;
    }

    public static String entriesToJson(List<ManifestEntry> entries) {
        return GSON.toJson(entries);
    }

    private static boolean shouldIncludeInManifest(CategoryType category, Path file) {
        return true;
    }

    private static ManifestEntry createEntry(CategoryType category, Path root, Path file, FileHashCache.ScopeSession hashCache) {
        try {
            String relativePath = FileUtils.toRelativeUnixPath(root, file);
            FileHashCache.FileFingerprint fingerprint = hashCache.describe(file, relativePath);
            return new ManifestEntry(
                    category,
                    relativePath,
                    file.getFileName().toString(),
                    fingerprint.size(),
                    fingerprint.sha256(),
                    true,
                    category.isDefaultRestartRequired(),
                    ""
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to build manifest entry for " + file, exception);
        }
    }

    private static void writeManifestCopy(ManifestData data, Path file) {
        try {
            FileUtils.ensureParentExists(file);
            Files.writeString(file, toJson(data), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LoggerUtils.error("Failed to write generated manifest copy", exception);
        }
    }
}
