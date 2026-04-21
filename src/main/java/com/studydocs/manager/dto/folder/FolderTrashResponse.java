package com.studydocs.manager.dto.folder;

import java.time.LocalDateTime;

public class FolderTrashResponse {
    private Long id;
    private String name;
    private Long parentId;
    private LocalDateTime deletedAt;
    private int deletedDescendantFolderCount;
    private int deletedDocumentCount;
    private boolean restorable;
    private String reason;
    private String message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public int getDeletedDescendantFolderCount() {
        return deletedDescendantFolderCount;
    }

    public void setDeletedDescendantFolderCount(int deletedDescendantFolderCount) {
        this.deletedDescendantFolderCount = deletedDescendantFolderCount;
    }

    public int getDeletedDocumentCount() {
        return deletedDocumentCount;
    }

    public void setDeletedDocumentCount(int deletedDocumentCount) {
        this.deletedDocumentCount = deletedDocumentCount;
    }

    public boolean isRestorable() {
        return restorable;
    }

    public void setRestorable(boolean restorable) {
        this.restorable = restorable;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
