package com.studydocs.manager.dto.file;

/**
 * PresignedUploadResponse - DTO trả về sau khi cấp Presigned Upload URL
 */
public class PresignedUploadResponse {
    private String uploadUrl;
    private String objectName;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private String httpMethod = "PUT";
    private int expiresInMinutes;

    public PresignedUploadResponse() {
    }

    public PresignedUploadResponse(String uploadUrl, String objectName, String fileName, Long fileSize, String contentType, int expiresInMinutes) {
        this.uploadUrl = uploadUrl;
        this.objectName = objectName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.expiresInMinutes = expiresInMinutes;
        this.httpMethod = "PUT";
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(int expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}
