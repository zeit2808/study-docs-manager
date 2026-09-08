package com.studydocs.manager.service.user;

import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(UserResponseMapper.class);
    private final StorageProvider storageProvider;

    public UserResponseMapper(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullname(user.getFullname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole() != null ? user.getRole().getName() : null);
        response.setAvatarObjectName(user.getAvatarObjectName());

        if (user.getAvatarObjectName() != null && !user.getAvatarObjectName().isBlank()) {
            try {
                // Presigned GET URL cho avatar (thời hạn 60 phút)
                response.setAvatarUrl(storageProvider.generatePresignedUrl(user.getAvatarObjectName(), 60));
            } catch (IOException e) {
                logger.warn("Failed to generate presigned avatar URL for user {}: {}", user.getUsername(), e.getMessage());
            }
        }

        return response;
    }
}
