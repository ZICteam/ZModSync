package com.modsync;

import java.util.ArrayList;
import java.util.List;

public class ManifestData {
    private long generatedAt;
    private List<ManifestEntry> entries = new ArrayList<>();

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<ManifestEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ManifestEntry> entries) {
        this.entries = entries;
    }
}
