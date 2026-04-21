package com.studydocs.manager.dto.folder;

public class FolderDeleteResult {
    private Long deletedFolderId;
    private int affectedDocuments;
    private String message;

    public FolderDeleteResult(Long folderId, int affectedDocuments) {
        this.deletedFolderId = folderId;
        this.affectedDocuments = affectedDocuments;
        this.message = affectedDocuments + " document(s) were moved to trash along with the folder tree.";
    }

    public Long getDeletedFolderId() {
        return deletedFolderId;
    }

    public void setDeletedFolderId(Long deletedFolderId) {
        this.deletedFolderId = deletedFolderId;
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
