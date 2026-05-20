package com.studydocs.manager.dto.subject;

import jakarta.validation.constraints.Size;

public class SubjectUpdateRequest {
    @Size(max = 200)
    private String name;
    @Size(max = 2000)
    private String description;

    private Long parentId;
    private Boolean isActive;
    private Integer sortOrder;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

}
