package com.studydocs.manager.dto.folder;

public class FolderDeleteResult {
    private Long deletedFolderId;
    private boolean cascade;
    private int affectedDocuments;
    private String message;
    public FolderDeleteResult(Long folderId, boolean cascade, int affectedDocuments) {
        this.deletedFolderId = folderId;
        this.cascade = cascade;
        this.affectedDocuments = affectedDocuments;
        this.message = cascade
                ? affectedDocuments + " document(s) were soft-deleted along with the folder."
                : affectedDocuments + " document(s) were unlinked and kept.";
    }

    public Long getDeletedFolderId() {
        return deletedFolderId;
    }

    public void setDeletedFolderId(Long deletedFolderId) {
        this.deletedFolderId = deletedFolderId;
    }

    public boolean isCascade() {
        return cascade;
    }

    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }

    public int getAffectedDocuments() {
        return affectedDocuments;
    }

    public void setAffectedDocuments(int affectedDocuments) {
        this.affectedDocuments = affectedDocuments;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
