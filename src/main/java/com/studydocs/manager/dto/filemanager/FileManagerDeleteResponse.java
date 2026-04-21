package com.studydocs.manager.dto.filemanager;

import java.util.ArrayList;
import java.util.List;

public class FileManagerDeleteResponse {
    private Long currentFolderId;
    private List<Long> deletedFolderIds = List.of();
    private List<Long> deletedDocumentIds = List.of();
    private int deletedFolderCount;
    private int deletedDocumentCount;
    private int cascadeDeletedDocumentCount;
    private int totalAffectedDocumentCount;
    private String message;

    public FileManagerDeleteResponse() {
    }

    public FileManagerDeleteResponse(
            Long currentFolderId,
            List<Long> deletedFolderIds,
            List<Long> deletedDocumentIds,
            int cascadeDeletedDocumentCount) {
        this.currentFolderId = currentFolderId;
        this.deletedFolderIds = deletedFolderIds == null ? List.of() : List.copyOf(deletedFolderIds);
        this.deletedDocumentIds = deletedDocumentIds == null ? List.of() : List.copyOf(deletedDocumentIds);
        this.deletedFolderCount = this.deletedFolderIds.size();
        this.deletedDocumentCount = this.deletedDocumentIds.size();
        this.cascadeDeletedDocumentCount = cascadeDeletedDocumentCount;
        this.totalAffectedDocumentCount = this.deletedDocumentCount + cascadeDeletedDocumentCount;
        this.message = buildMessage();
    }

    public Long getCurrentFolderId() {
        return currentFolderId;
    }

    public void setCurrentFolderId(Long currentFolderId) {
        this.currentFolderId = currentFolderId;
    }

    public List<Long> getDeletedFolderIds() {
        return deletedFolderIds;
    }

    public void setDeletedFolderIds(List<Long> deletedFolderIds) {
        this.deletedFolderIds = deletedFolderIds == null ? List.of() : List.copyOf(deletedFolderIds);
        this.deletedFolderCount = this.deletedFolderIds.size();
        this.message = buildMessage();
    }

    public List<Long> getDeletedDocumentIds() {
        return deletedDocumentIds;
    }

    public void setDeletedDocumentIds(List<Long> deletedDocumentIds) {
        this.deletedDocumentIds = deletedDocumentIds == null ? List.of() : List.copyOf(deletedDocumentIds);
        this.deletedDocumentCount = this.deletedDocumentIds.size();
        this.totalAffectedDocumentCount = this.deletedDocumentCount + this.cascadeDeletedDocumentCount;
        this.message = buildMessage();
    }

    public int getDeletedFolderCount() {
        return deletedFolderCount;
    }

    public void setDeletedFolderCount(int deletedFolderCount) {
        this.deletedFolderCount = deletedFolderCount;
    }

    public int getDeletedDocumentCount() {
        return deletedDocumentCount;
    }

    public void setDeletedDocumentCount(int deletedDocumentCount) {
        this.deletedDocumentCount = deletedDocumentCount;
    }

    public int getCascadeDeletedDocumentCount() {
        return cascadeDeletedDocumentCount;
    }

    public void setCascadeDeletedDocumentCount(int cascadeDeletedDocumentCount) {
        this.cascadeDeletedDocumentCount = cascadeDeletedDocumentCount;
        this.totalAffectedDocumentCount = this.deletedDocumentCount + cascadeDeletedDocumentCount;
        this.message = buildMessage();
    }

    public int getTotalAffectedDocumentCount() {
        return totalAffectedDocumentCount;
    }

    public void setTotalAffectedDocumentCount(int totalAffectedDocumentCount) {
        this.totalAffectedDocumentCount = totalAffectedDocumentCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String buildMessage() {
        List<String> parts = new ArrayList<>();
        if (deletedFolderCount > 0) {
            parts.add(deletedFolderCount + " folder(s)");
        }
        if (deletedDocumentCount > 0) {
            parts.add(deletedDocumentCount + " document(s)");
        }
        if (parts.isEmpty()) {
            return "No items were deleted.";
        }

        String baseMessage = "Deleted " + String.join(" and ", parts) + ".";
        if (cascadeDeletedDocumentCount > 0) {
            return baseMessage + " " + cascadeDeletedDocumentCount
                    + " nested document(s) were also moved to trash from the selected folder tree(s).";
        }
        return baseMessage;
    }
}
