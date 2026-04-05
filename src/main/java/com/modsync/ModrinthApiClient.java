package com.modsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModrinthApiClient {
    private static final String API_BASE_URL = "https://api.modrinth.com/v2";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final Map<String, String> SHA1_TO_URL_CACHE = new ConcurrentHashMap<>();
    private static final String NO_RESULT = "<none>";

    private ModrinthApiClient() {
    }

    public static String resolveDownloadUrlIfEnabled(ManifestEntry entry) {
        if (!ConfigManager.enableModrinthCdnFallbackOrDefault()) {
            return null;
        }
        return resolveDownloadUrl(entry, API_BASE_URL, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    static String resolveDownloadUrl(ManifestEntry entry, String apiBaseUrl, int connectTimeoutMs, int readTimeoutMs) {
        if (entry == null
                || entry.getCategory() != CategoryType.MOD
                || entry.getSha1() == null
                || entry.getSha1().isBlank()) {
            return null;
        }

        String sha1 = entry.getSha1().trim().toLowerCase();
        String cached = SHA1_TO_URL_CACHE.get(sha1);
        if (cached != null) {
            return NO_RESULT.equals(cached) ? null : cached;
        }

        String resolved = fetchDownloadUrl(entry, apiBaseUrl, connectTimeoutMs, readTimeoutMs);
        SHA1_TO_URL_CACHE.put(sha1, resolved == null ? NO_RESULT : resolved);
        return resolved;
    }

    static void resetCacheForTests() {
        SHA1_TO_URL_CACHE.clear();
    }

    private static String fetchDownloadUrl(ManifestEntry entry, String apiBaseUrl, int connectTimeoutMs, int readTimeoutMs) {
        HttpURLConnection connection = null;
        try {
            String requestUrl = apiBaseUrl
                    + "/version_file/"
                    + URLEncoder.encode(entry.getSha1().trim().toLowerCase(), StandardCharsets.UTF_8)
                    + "?algorithm=sha1";
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "SyncBridge/" + ModSync.MOD_ID);

            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return null;
            }
            if (responseCode < 200 || responseCode >= 300) {
                LoggerUtils.warn("Modrinth lookup failed for " + entry.getRelativePath() + ": HTTP " + responseCode);
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return selectMatchingFileUrl(root, entry.getSha1());
            }
        } catch (Exception exception) {
            LoggerUtils.warn("Modrinth lookup failed for " + entry.getRelativePath() + ": " + exception.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String selectMatchingFileUrl(JsonObject root, String expectedSha1) {
        if (root == null || !root.has("files") || !root.get("files").isJsonArray()) {
            return null;
        }

        String normalizedSha1 = expectedSha1 == null ? "" : expectedSha1.trim().toLowerCase();
        JsonArray files = root.getAsJsonArray("files");
        String firstMatchingUrl = null;

        for (JsonElement fileElement : files) {
            if (!fileElement.isJsonObject()) {
                continue;
            }
            JsonObject file = fileElement.getAsJsonObject();
            JsonObject hashes = file.has("hashes") && file.get("hashes").isJsonObject()
                    ? file.getAsJsonObject("hashes")
                    : null;
            String sha1 = hashes != null && hashes.has("sha1") ? hashes.get("sha1").getAsString() : "";
            if (!normalizedSha1.equalsIgnoreCase(sha1)) {
                continue;
            }

            String url = file.has("url") ? file.get("url").getAsString() : "";
            if (url.isBlank()) {
                continue;
            }
            if (file.has("primary") && file.get("primary").getAsBoolean()) {
                return url;
            }
            if (firstMatchingUrl == null) {
                firstMatchingUrl = url;
            }
        }

        return firstMatchingUrl;
    }
}
