package com.studydocs.manager.dto;

import java.util.List;

/**
 * BatchUploadResponse - Response cho batch upload nhiều files
 * 
 * Chứa kết quả upload của tất cả các file, bao gồm cả thành công và thất bại
 * Thứ tự của results giữ nguyên như thứ tự của files trong request
 */
public class BatchUploadResponse {
    private int totalFiles; // Tổng số file được gửi lên
    private int successCount; // Số file upload thành công
    private int failureCount; // Số file upload thất bại
    private List<FileUploadResult> results; // Kết quả từng file (theo thứ tự request)

    // Constructors
    public BatchUploadResponse() {
    }

    public BatchUploadResponse(int totalFiles, int successCount, int failureCount, List<FileUploadResult> results) {
        this.totalFiles = totalFiles;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    // Getters and Setters
    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<FileUploadResult> getResults() {
        return results;
    }

    public void setResults(List<FileUploadResult> results) {
        this.results = results;
    }
}
