package com.studydocs.manager.dto;

/**
 * FileUploadResult - Kết quả upload của 1 file trong batch upload
 * 
 * Lưu thông tin về việc upload thành công hay thất bại của từng file
 * Giữ nguyên thứ tự của file trong request để frontend dễ xử lý
 */
public class FileUploadResult {
    private String originalFileName; // Tên file gốc từ request
    private int index; // Vị trí của file trong request (0-based)
    private boolean success; // true = upload thành công, false = thất bại
    private FileUploadResponse data; // Dữ liệu file nếu upload thành công (null nếu thất bại)
    private String errorMessage; // Thông báo lỗi nếu upload thất bại (null nếu thành công)

    // Constructors
    public FileUploadResult() {
    }

    /**
     * Constructor cho trường hợp upload thành công
     */
    public FileUploadResult(String originalFileName, int index, FileUploadResponse data) {
        this.originalFileName = originalFileName;
        this.index = index;
        this.success = true;
        this.data = data;
        this.errorMessage = null;
    }

    /**
     * Constructor cho trường hợp upload thất bại
     */
    public FileUploadResult(String originalFileName, int index, String errorMessage) {
        this.originalFileName = originalFileName;
        this.index = index;
        this.success = false;
        this.data = null;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public FileUploadResponse getData() {
        return data;
    }

    public void setData(FileUploadResponse data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
