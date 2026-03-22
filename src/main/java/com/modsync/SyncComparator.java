package com.modsync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SyncComparator {
    private SyncComparator() {
    }

    public static List<ManifestEntry> findMissingOrOutdated(List<ManifestEntry> localEntries, ManifestData manifestData) {
        Map<String, ManifestEntry> localMap = new HashMap<>();
        for (ManifestEntry entry : localEntries) {
            localMap.put(entry.getIdentityKey(), entry);
        }

        return manifestData.getEntries().stream()
                .filter(serverEntry -> isMissingOrDifferent(serverEntry, localMap.get(serverEntry.getIdentityKey())))
                .collect(Collectors.toList());
    }

    private static boolean isMissingOrDifferent(ManifestEntry serverEntry, ManifestEntry clientEntry) {
        if (clientEntry == null) {
            return true;
        }
        return clientEntry.getFileSize() != serverEntry.getFileSize()
                || !clientEntry.getSha256().equalsIgnoreCase(serverEntry.getSha256());
    }
}
