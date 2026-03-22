package com.modsync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        String host = extractHost(serverAddress);
        IOException lastError = null;
        List<String> attemptedUrls = new ArrayList<>();

        for (String url : buildManifestCandidateUrls(host, discoveredPort)) {
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
        if (serverAddress == null || serverAddress.isBlank()) {
            return "127.0.0.1";
        }

        String address = serverAddress.trim();
        if (address.startsWith("[")) {
            int end = address.indexOf(']');
            if (end > 0) {
                return address.substring(1, end);
            }
        }

        int colonCount = 0;
        for (int i = 0; i < address.length(); i++) {
            if (address.charAt(i) == ':') {
                colonCount++;
            }
        }

        if (colonCount == 1) {
            int index = address.lastIndexOf(':');
            return address.substring(0, index);
        }

        return address;
    }

    public static String normalizeHost(String host) {
        if (host.contains(":") && !host.startsWith("[")) {
            return "[" + host + "]";
        }
        return host;
    }

    public static String resolvePublicBaseUrl(String serverHost) {
        String configured = ConfigManager.publicHttpBaseUrl();
        if (!configured.isBlank()) {
            return stripTrailingSlash(configured);
        }
        return "http://" + normalizeHost(serverHost) + ":" + ConfigManager.serverHttpPort();
    }

    private static List<String> buildManifestCandidateUrls(String host, int discoveredPort) {
        Set<String> urls = new LinkedHashSet<>();
        String normalizedHost = normalizeHost(host);

        if (discoveredPort > 0) {
            urls.add("http://" + normalizedHost + ":" + discoveredPort + "/manifest");
        }
        urls.add("http://" + normalizedHost + ":" + ConfigManager.serverHttpPort() + "/manifest");
        urls.add("https://" + normalizedHost + "/manifest");
        urls.add("http://" + normalizedHost + "/manifest");
        return new ArrayList<>(urls);
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
