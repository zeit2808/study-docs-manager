package com.studydocs.manager.dto.filemanager;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class FileManagerTransferRequest {

    private Long targetFolderId;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<FileManagerPasteItemRequest> items;

    public Long getTargetFolderId() {
        return targetFolderId;
    }

    public void setTargetFolderId(Long targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    public List<FileManagerPasteItemRequest> getItems() {
        return items;
    }

    public void setItems(List<FileManagerPasteItemRequest> items) {
        this.items = items;
    }
}
