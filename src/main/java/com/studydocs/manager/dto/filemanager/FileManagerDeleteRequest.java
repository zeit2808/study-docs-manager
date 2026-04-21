package com.studydocs.manager.dto.filemanager;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class FileManagerDeleteRequest {

    @Positive(message = "currentFolderId must be greater than 0")
    private Long currentFolderId;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<FileManagerPasteItemRequest> items;

    public Long getCurrentFolderId() {
        return currentFolderId;
    }

    public void setCurrentFolderId(Long currentFolderId) {
        this.currentFolderId = currentFolderId;
    }

    public List<FileManagerPasteItemRequest> getItems() {
        return items;
    }

    public void setItems(List<FileManagerPasteItemRequest> items) {
        this.items = items;
    }
}
