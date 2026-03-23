package com.modsync;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
