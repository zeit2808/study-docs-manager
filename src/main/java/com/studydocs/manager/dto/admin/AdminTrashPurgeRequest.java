package com.studydocs.manager.dto.admin;

import jakarta.validation.constraints.NotNull;

public class AdminTrashPurgeRequest {

    public enum Scope {
        DOCUMENTS,
        FOLDERS,
        ALL
    }

    @NotNull
    private Scope scope;

    private Long userId;

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
