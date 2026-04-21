package com.studydocs.manager.dto.filemanager;

import com.studydocs.manager.enums.ClipboardOperation;

import java.util.ArrayList;
import java.util.List;

public class FileManagerPasteResponse {

    private ClipboardOperation operation;
    private Long targetFolderId;
    private int folderCount;
    private int documentCount;
    private List<FileManagerPasteResult> results = new ArrayList<>();

    public ClipboardOperation getOperation() {
        return operation;
    }

    public void setOperation(ClipboardOperation operation) {
        this.operation = operation;
    }

    public Long getTargetFolderId() {
        return targetFolderId;
    }

    public void setTargetFolderId(Long targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    public int getFolderCount() {
        return folderCount;
    }

    public void setFolderCount(int folderCount) {
        this.folderCount = folderCount;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public List<FileManagerPasteResult> getResults() {
        return results;
    }

    public void setResults(List<FileManagerPasteResult> results) {
        this.results = results;
    }
}
