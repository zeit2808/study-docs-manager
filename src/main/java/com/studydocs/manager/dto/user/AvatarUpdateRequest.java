package com.studydocs.manager.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AvatarUpdateRequest - DTO nhận objectName của avatar sau khi client upload lên MinIO
 */
public class AvatarUpdateRequest {

    @NotBlank(message = "Avatar object name must not be blank")
    @Size(max = 500, message = "Avatar object name must not exceed 500 characters")
    private String avatarObjectName;

    public AvatarUpdateRequest() {
    }

    public AvatarUpdateRequest(String avatarObjectName) {
        this.avatarObjectName = avatarObjectName;
    }

    public String getAvatarObjectName() {
        return avatarObjectName;
    }

    public void setAvatarObjectName(String avatarObjectName) {
        this.avatarObjectName = avatarObjectName;
    }
}
