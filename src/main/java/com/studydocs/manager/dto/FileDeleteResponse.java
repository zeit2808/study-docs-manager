package com.studydocs.manager.dto;

/**
 * FileDeleteResponse - DTO trả về sau khi xóa file thành công
 * 
 * Chứa thông tin về file đã bị xóa và trạng thái thành công
 */
public class FileDeleteResponse {
    private boolean success;
    private String message;
    private String deletedObjectName;

    // Constructors
    public FileDeleteResponse() {
    }

    public FileDeleteResponse(boolean success, String message, String deletedObjectName) {
        this.success = success;
        this.message = message;
        this.deletedObjectName = deletedObjectName;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeletedObjectName() {
        return deletedObjectName;
    }

    public void setDeletedObjectName(String deletedObjectName) {
        this.deletedObjectName = deletedObjectName;
    }
}
