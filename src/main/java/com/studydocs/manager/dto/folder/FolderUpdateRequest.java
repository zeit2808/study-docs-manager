package com.studydocs.manager.dto.folder;

import jakarta.validation.constraints.Size;

public class FolderUpdateRequest {
    @Size(max = 200)
    private String name;
    private Long parentId;   // null = move to root
    private Integer sortOrder;
    private boolean parentIdProvided;

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
        this.parentIdProvided = true;
    }

    public boolean isParentIdProvided() {
        return parentIdProvided;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
