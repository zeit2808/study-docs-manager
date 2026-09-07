package com.studydocs.manager.dto.file;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PresignedUploadRequest - DTO gửi lên để xin cấp Presigned Upload URL
 */
public class PresignedUploadRequest {

    @NotBlank(message = "File name must not be blank")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @NotNull(message = "File size must not be null")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSize;

    @NotBlank(message = "Content type must not be blank")
    private String contentType;

    /**
     * Folder type: DOCUMENTS, THUMBNAILS, AVATARS (default: DOCUMENTS)
     */
    private String folderType = "DOCUMENTS";

    public PresignedUploadRequest() {
    }

    public PresignedUploadRequest(String fileName, Long fileSize, String contentType, String folderType) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.folderType = folderType;
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

    public String getFolderType() {
        return folderType;
    }

    public void setFolderType(String folderType) {
        this.folderType = folderType;
    }
}
