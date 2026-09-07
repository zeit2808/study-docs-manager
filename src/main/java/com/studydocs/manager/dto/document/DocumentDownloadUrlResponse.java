package com.studydocs.manager.dto.document;

/**
 * DocumentDownloadUrlResponse - DTO trả về khi yêu cầu download URL của tài liệu
 */
public class DocumentDownloadUrlResponse {
    private String downloadUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private int expiresInMinutes;

    public DocumentDownloadUrlResponse() {
    }

    public DocumentDownloadUrlResponse(String downloadUrl, String fileName, String fileType, Long fileSize, int expiresInMinutes) {
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.expiresInMinutes = expiresInMinutes;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(int expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}
