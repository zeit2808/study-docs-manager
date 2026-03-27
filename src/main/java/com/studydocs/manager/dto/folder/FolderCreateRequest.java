package com.studydocs.manager.dto.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FolderCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String name;
    private Long parentId;   // null = root folder
    private String color;    // hex color, e.g. "#FF5733"
    private String icon;     // icon name, e.g. "folder-open"
    private Integer sortOrder;
    // getters/setters...

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
