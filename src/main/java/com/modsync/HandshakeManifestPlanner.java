package com.modsync;

import java.util.List;
import java.util.Objects;

public final class HandshakeManifestPlanner {
    record ServerHandshakeResponse(ManifestData outboundManifest, List<ManifestEntry> requiredEntries) {
    }

    record StartDownloadPlan(List<ManifestEntry> requiredEntries,
                             boolean alreadySynchronized,
                             boolean saveManagedManifest,
                             boolean startDownloads) {
    }

    private HandshakeManifestPlanner() {
    }

    static ManifestData ensureServerManifest(ManifestData manifestData, long generatedAt) {
        if (manifestData != null) {
            return manifestData;
        }

        ManifestData emptyManifest = new ManifestData();
        emptyManifest.setGeneratedAt(generatedAt);
        emptyManifest.setEntries(List.of());
        return emptyManifest;
    }

    static ManifestData attachDownloadUrls(ManifestData manifestData, String baseUrl) {
        ManifestData copy = new ManifestData();
        copy.setGeneratedAt(manifestData.getGeneratedAt());
        List<ManifestEntry> entries = manifestData.getEntries().stream().map(entry -> {
            ManifestEntry cloned = entry.copy();
            cloned.setDownloadUrl(baseUrl + "/files/" + entry.getCategory().getHttpSegment() + "/"
                    + HttpPathCodec.encodeRelativePath(entry.getRelativePath()));
            return cloned;
        }).toList();
        copy.setEntries(entries);
        return copy;
    }

    static ServerHandshakeResponse buildServerHandshakeResponse(List<ManifestEntry> clientEntries,
                                                                ManifestData manifestData,
                                                                String baseUrl) {
        ManifestData outboundManifest = attachDownloadUrls(manifestData, baseUrl);
        List<ManifestEntry> requiredEntries = SyncComparator.findMissingOrOutdated(clientEntries, outboundManifest);
        return new ServerHandshakeResponse(outboundManifest, requiredEntries);
    }

    static List<ManifestEntry> determineRequiredDownloads(List<ManifestEntry> serverSuggested,
                                                          ManifestData manifestData,
                                                          List<ManifestEntry> localEntries) {
        return manifestData == null
                ? sanitizeDownloadableEntries(serverSuggested)
                : SyncComparator.findMissingOrOutdated(localEntries, manifestData);
    }

    static StartDownloadPlan buildStartDownloadPlan(List<ManifestEntry> serverSuggested,
                                                    ManifestData manifestData,
                                                    List<ManifestEntry> localEntries) {
        List<ManifestEntry> requiredEntries = determineRequiredDownloads(serverSuggested, manifestData, localEntries);
        boolean alreadySynchronized = requiredEntries.isEmpty();
        return new StartDownloadPlan(
                requiredEntries,
                alreadySynchronized,
                manifestData != null,
                !alreadySynchronized
        );
    }

    static List<ManifestEntry> sanitizeDownloadableEntries(List<ManifestEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(Objects::nonNull)
                .filter(HandshakeManifestPlanner::hasUsableDownloadMetadata)
                .toList();
    }

    private static boolean hasUsableDownloadMetadata(ManifestEntry entry) {
        return entry.getCategory() != null
                && entry.getRelativePath() != null
                && !entry.getRelativePath().isBlank()
                && entry.getSha256() != null
                && !entry.getSha256().isBlank()
                && entry.getDownloadUrl() != null
                && !entry.getDownloadUrl().isBlank();
    }
}
