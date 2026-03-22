package com.modsync;

public class ManifestEntry {
    private CategoryType category;
    private String relativePath;
    private String fileName;
    private long fileSize;
    private String sha256;
    private boolean required;
    private boolean restartRequired;
    private String downloadUrl;

    public ManifestEntry() {
    }

    public ManifestEntry(CategoryType category, String relativePath, String fileName, long fileSize,
                         String sha256, boolean required, boolean restartRequired, String downloadUrl) {
        this.category = category;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.required = required;
        this.restartRequired = restartRequired;
        this.downloadUrl = downloadUrl;
    }

    public CategoryType getCategory() {
        return category;
    }

    public void setCategory(CategoryType category) {
        this.category = category;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    public void setRestartRequired(boolean restartRequired) {
        this.restartRequired = restartRequired;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getIdentityKey() {
        return category.name() + ":" + relativePath;
    }

    public ManifestEntry copy() {
        return new ManifestEntry(category, relativePath, fileName, fileSize, sha256, required, restartRequired, downloadUrl);
    }
}
