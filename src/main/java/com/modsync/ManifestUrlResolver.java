package com.modsync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URI;
import java.net.URISyntaxException;

final class ManifestUrlResolver {
    private ManifestUrlResolver() {
    }

    static String extractHost(String serverAddress) {
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

    static String normalizeHost(String host) {
        if (host.contains(":") && !host.startsWith("[")) {
            return "[" + host + "]";
        }
        return host;
    }

    static String resolvePublicBaseUrl(String configuredBaseUrl, String serverHost, int serverHttpPort) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return stripTrailingSlash(configuredBaseUrl);
        }
        return "http://" + normalizeHost(serverHost) + ":" + serverHttpPort;
    }

    static List<String> buildManifestCandidateUrls(String host, int discoveredPort, int serverHttpPort) {
        Set<String> urls = new LinkedHashSet<>();
        String normalizedHost = normalizeHost(host);

        if (discoveredPort > 0) {
            urls.add("http://" + normalizedHost + ":" + discoveredPort + "/manifest");
        }
        urls.add("http://" + normalizedHost + ":" + serverHttpPort + "/manifest");
        urls.add("https://" + normalizedHost + "/manifest");
        urls.add("http://" + normalizedHost + "/manifest");
        return new ArrayList<>(urls);
    }

    static List<String> buildDownloadCandidateUrls(ManifestEntry entry,
                                                   String primaryDownloadUrl,
                                                   String serverAddress,
                                                   int discoveredPort) {
        Map<String, Boolean> urls = new LinkedHashMap<>();
        addCandidate(urls, primaryDownloadUrl);

        String host = extractHost(serverAddress);
        if (!host.isBlank()) {
            URI primaryUri = parseUri(primaryDownloadUrl);
            if (primaryUri != null && primaryUri.getScheme() != null && primaryUri.getRawPath() != null) {
                int port = primaryUri.getPort();
                if (port > 0) {
                    addCandidate(urls, primaryUri.getScheme() + "://" + normalizeHost(host) + ":" + port + primaryUri.getRawPath());
                }
            }

            if (discoveredPort > 0 && entry != null && entry.getCategory() != null
                    && entry.getRelativePath() != null && !entry.getRelativePath().isBlank()) {
                addCandidate(urls,
                        "http://" + normalizeHost(host) + ":" + discoveredPort + "/files/"
                                + entry.getCategory().getHttpSegment() + "/"
                                + HttpPathCodec.encodeRelativePath(entry.getRelativePath()));
            }
        }

        return new ArrayList<>(urls.keySet());
    }

    private static URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new URI(value);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private static void addCandidate(Map<String, Boolean> urls, String candidate) {
        if (candidate != null && !candidate.isBlank()) {
            urls.put(candidate, Boolean.TRUE);
        }
    }

    static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
