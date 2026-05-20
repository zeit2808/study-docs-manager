package com.studydocs.manager.dto.tag;

import jakarta.validation.constraints.Size;

public class TagUpdateRequest {
    @Size(max =100)
    private String name;
    private Boolean isActive;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
