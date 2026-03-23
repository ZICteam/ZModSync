package com.modsync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ServerManifestHttpHandler {
    private ServerManifestHttpHandler() {
    }

    public static ManifestData buildPublicManifest(String serverHost) {
        ManifestData manifest = ModSync.getLastManifest();
        if (manifest == null) {
            LoggerUtils.warn("Requested public manifest before cached manifest was ready");
            manifest = new ManifestData();
            manifest.setGeneratedAt(System.currentTimeMillis());
            manifest.setEntries(List.of());
        }
        ManifestData publicManifest = new ManifestData();
        publicManifest.setGeneratedAt(manifest.getGeneratedAt());
        String publicBaseUrl = resolvePublicBaseUrl(serverHost);

        List<ManifestEntry> entries = manifest.getEntries().stream().map(entry -> {
            ManifestEntry copy = entry.copy();
            copy.setDownloadUrl(publicBaseUrl + "/files/" + entry.getCategory().getHttpSegment() + "/" + entry.getRelativePath());
            return copy;
        }).toList();
        publicManifest.setEntries(entries);
        return publicManifest;
    }

    public static byte[] manifestBytes(String serverHost) {
        return ManifestGenerator.toJson(buildPublicManifest(serverHost)).getBytes(StandardCharsets.UTF_8);
    }

    public static ManifestData fetchManifest(String serverAddress) throws IOException {
        return fetchManifest(serverAddress, -1);
    }

    public static ManifestData fetchManifest(String serverAddress, int discoveredPort) throws IOException {
        return fetchManifest(serverAddress, discoveredPort, 5_000, 15_000);
    }

    public static ManifestData fetchManifest(String serverAddress, int discoveredPort, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        String host = ManifestUrlResolver.extractHost(serverAddress);
        IOException lastError = null;
        List<String> attemptedUrls = new ArrayList<>();

        for (String url : ManifestUrlResolver.buildManifestCandidateUrls(host, discoveredPort, ConfigManager.serverHttpPort())) {
            attemptedUrls.add(url);
            try {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                connection.setConnectTimeout(connectTimeoutMs);
                connection.setReadTimeout(readTimeoutMs);
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("HTTP " + responseCode);
                }

                try (InputStream stream = connection.getInputStream()) {
                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    LoggerUtils.info("Manifest fetched from " + url);
                    return ManifestGenerator.fromJson(json);
                }
            } catch (IOException exception) {
                lastError = exception;
            }
        }

        String suffix = lastError == null ? "no response details" : lastError.getMessage();
        throw new IOException("Unable to fetch ModSync manifest from " + String.join(", ", attemptedUrls) + " (" + suffix + ")", lastError);
    }

    public static String extractHost(String serverAddress) {
        return ManifestUrlResolver.extractHost(serverAddress);
    }

    public static String normalizeHost(String host) {
        return ManifestUrlResolver.normalizeHost(host);
    }

    public static String resolvePublicBaseUrl(String serverHost) {
        return ManifestUrlResolver.resolvePublicBaseUrl(ConfigManager.publicHttpBaseUrl(), serverHost, ConfigManager.serverHttpPort());
    }
}
