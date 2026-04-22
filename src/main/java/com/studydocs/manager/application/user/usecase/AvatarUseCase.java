package com.studydocs.manager.application.user.usecase;

import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.ServiceUnavailableException;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.file.FileValidationService;
import com.studydocs.manager.service.user.UserResponseMapper;
import com.studydocs.manager.storage.StorageProvider;
import com.studydocs.manager.storage.StoredFile;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class AvatarUseCase {

    private static final List<String> ALLOWED_AVATAR_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    private static final List<String> ALLOWED_AVATAR_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp");

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final UserResponseMapper userResponseMapper;
    private final FileValidationService fileValidationService;

    public AvatarUseCase(
            UserRepository userRepository,
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            UserResponseMapper userResponseMapper,
            FileValidationService fileValidationService) {
        this.userRepository = userRepository;
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.userResponseMapper = userResponseMapper;
        this.fileValidationService = fileValidationService;
    }

    @Transactional
    public UserResponse updateAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "username"));

        try {
            validateAvatar(file);

            if (user.getAvatarObjectName() != null && !user.getAvatarObjectName().isEmpty()) {
                storageProvider.deleteFile(user.getAvatarObjectName());
            }

            String avatarFolder = buildAvatarFolder(user.getId());
            StoredFile storedFile = storageProvider.uploadFile(file, avatarFolder);
            user.setAvatarObjectName(storedFile.objectName());

            User updatedUser = userRepository.save(user);
            return userResponseMapper.toResponse(updatedUser);
        } catch (IOException e) {
            throw new ServiceUnavailableException(
                    "Failed to upload avatar: " + e.getMessage(),
                    "AVATAR_UPLOAD_FAILED",
                    "file");
        }
    }

    private void validateAvatar(MultipartFile file) {
        fileValidationService.validateContentTypeAndSize(
                file,
                ALLOWED_AVATAR_TYPES,
                MAX_AVATAR_SIZE,
                "Invalid avatar type. Allowed types: " + ALLOWED_AVATAR_TYPES,
                "INVALID_AVATAR_TYPE",
                "Avatar size exceeds maximum allowed size of 5MB",
                "AVATAR_SIZE_EXCEEDED",
                "file");
        fileValidationService.validateImageExtension(
                file,
                ALLOWED_AVATAR_EXTENSIONS,
                "Invalid avatar extension. Allowed extensions: " + ALLOWED_AVATAR_EXTENSIONS,
                "INVALID_AVATAR_EXTENSION",
                "file");
        fileValidationService.validateImageSignature(
                file,
                "Uploaded file is not a valid image",
                "INVALID_AVATAR_CONTENT",
                "Could not read uploaded avatar",
                "file");
    }

    private String buildAvatarFolder(Long userId) {
        return storageProperties.getAvatarsFolder() + userId + "/";
    }
}
