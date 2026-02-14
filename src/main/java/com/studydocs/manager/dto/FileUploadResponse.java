package com.studydocs.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * FileUploadResponse - DTO trả về sau khi upload file thành công
 * 
 * Chứa thông tin file sau khi upload lên MinIO
 * Client nhận response này để biết URL file đã upload
 */
public class FileUploadResponse {
    private String fileUrl; // URL để truy cập file
    private String fileName; // Tên file gốc
    private Long fileSize; // Kích thước file (bytes)
    private String fileType; // MIME type (vd: application/pdf)

    private String objectName; // Đường dẫn object trong MinIO

    @JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ hiển thị khi có giá trị
    private FileMetadataSummary metadata; // Lightweight metadata (chỉ có khi upload document với extractMetadata=true)

    // Constructors
    public FileUploadResponse() {
    }

    public FileUploadResponse(String fileUrl, String fileName, Long fileSize, String fileType) {
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public FileMetadataSummary getMetadata() {
        return metadata;
    }

    public void setMetadata(FileMetadataSummary metadata) {
        this.metadata = metadata;
    }
}
