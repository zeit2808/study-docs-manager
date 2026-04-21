package com.studydocs.manager.dto.filemanager;

import com.studydocs.manager.enums.FileManagerItemType;
import jakarta.validation.constraints.NotNull;

public class FileManagerPasteItemRequest {

    @NotNull(message = "Item type is required")
    private FileManagerItemType type;

    @NotNull(message = "Item id is required")
    private Long id;

    public FileManagerItemType getType() {
        return type;
    }

    public void setType(FileManagerItemType type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
