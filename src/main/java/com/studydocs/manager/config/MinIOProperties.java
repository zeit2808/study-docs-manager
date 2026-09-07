package com.studydocs.manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
public class MinIOProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String documentsFolder;
    private String thumbnailsFolder;
    private String externalEndpoint;
    private int presignedUploadExpiryMinutes = 15;
    private int presignedDownloadExpiryMinutes = 30;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getExternalEndpoint() {
        if (externalEndpoint != null && !externalEndpoint.isBlank()) {
            return externalEndpoint;
        }
        return endpoint;
    }

    public void setExternalEndpoint(String externalEndpoint) {
        this.externalEndpoint = externalEndpoint;
    }

    public int getPresignedUploadExpiryMinutes() {
        return presignedUploadExpiryMinutes;
    }

    public void setPresignedUploadExpiryMinutes(int presignedUploadExpiryMinutes) {
        this.presignedUploadExpiryMinutes = presignedUploadExpiryMinutes;
    }

    public int getPresignedDownloadExpiryMinutes() {
        return presignedDownloadExpiryMinutes;
    }

    public void setPresignedDownloadExpiryMinutes(int presignedDownloadExpiryMinutes) {
        this.presignedDownloadExpiryMinutes = presignedDownloadExpiryMinutes;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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
