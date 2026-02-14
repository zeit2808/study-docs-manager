package com.studydocs.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * StorageProperties - Configuration properties cho storage layer
 * 
 * Cho phép configure storage provider và các thông số liên quan
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Storage provider đang sử dụng
     * Giá trị: "minio", "s3", "gcs", "azure", etc.
     */
    private String provider = "minio";

    /**
     * Folder lưu documents
     */
    private String documentsFolder = "documents/";

    /**
     * Folder lưu thumbnails
     */
    private String thumbnailsFolder = "thumbnails/";

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getDocumentsFolder() {
        return documentsFolder;
    }

    public void setDocumentsFolder(String documentsFolder) {
        this.documentsFolder = documentsFolder;
    }

    public String getThumbnailsFolder() {
        return thumbnailsFolder;
    }

    public void setThumbnailsFolder(String thumbnailsFolder) {
        this.thumbnailsFolder = thumbnailsFolder;
    }
}
