package com.studydocs.manager.service.user;

import com.studydocs.manager.dto.user.ProfileUpdateRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.dto.user.UserUpdateRequest;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.ServiceUnavailableException;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import com.studydocs.manager.service.document.AuditLogService;
import com.studydocs.manager.storage.StorageProvider;
import com.studydocs.manager.storage.StoredFile;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UserService {
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
    private final PasswordEncoder passwordEncoder;
    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final UserResponseMapper userResponseMapper;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            RoleRepository roleRepository,
            AuditLogService auditLogService,
            SecurityUtils securityUtils,
            UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.userResponseMapper = userResponseMapper;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "id"));
        return userResponseMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "id"));
        User before = new User();
        before.setEmail(user.getEmail());
        before.setFullname(user.getFullname());
        before.setPhone(user.getPhone());
        before.setEnabled(user.getEnabled());

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty()) {
            if (!user.getEmail().equals(updateRequest.getEmail()) &&
                    userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new ConflictException("Email is already in use", "EMAIL_TAKEN", "email");
            }
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        if (updateRequest.getFullname() != null) {
            user.setFullname(updateRequest.getFullname());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getEnabled() != null) {
            user.setEnabled(updateRequest.getEnabled());
        }
        if (updateRequest.getRole() != null && !updateRequest.getRole().isBlank()) {
            Role role = roleRepository.findByName(updateRequest.getRole().toUpperCase())
                    .orElseThrow(() -> new NotFoundException(
                            "Role " + updateRequest.getRole() + " not found",
                            "ROLE_NOT_FOUND",
                            "role"));
            user.setRole(role);
        }

        User updatedUser = userRepository.save(user);

        StringBuilder details = new StringBuilder();
        details.append("Admin updated user ").append(id).append(". ");
        if (!equals(before.getEmail(), updatedUser.getEmail())) {
            details.append(String.format("email: '%s' -> '%s'; ", before.getEmail(), updatedUser.getEmail()));
        }
        if (!equals(before.getFullname(), updatedUser.getFullname())) {
            details.append(String.format("fullname: '%s' -> '%s'; ", before.getFullname(), updatedUser.getFullname()));
        }
        if (!equals(before.getPhone(), updatedUser.getPhone())) {
            details.append(String.format("phone: '%s' -> '%s'; ", before.getPhone(), updatedUser.getPhone()));
        }
        if (!equals(before.getEnabled(), updatedUser.getEnabled())) {
            details.append(String.format("enabled: %s -> %s; ", before.getEnabled(), updatedUser.getEnabled()));
        }

        Long actorId = securityUtils.getCurrentUserId();
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();
        auditLogService.log(actorId, updatedUser.getId(), AuditAction.UPDATE_USER, details.toString(), ip, userAgent);
        return userResponseMapper.toResponse(updatedUser);
    }

    private boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found", "USER_NOT_FOUND", "id");
        }
        userRepository.deleteById(id);
        Long actorId = securityUtils.getCurrentUserId();
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();
        auditLogService.log(actorId, id, AuditAction.DELETE_USER, "Deleted user with id " + id, ip, userAgent);
    }

    @Transactional
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "username"));
        return userResponseMapper.toResponse(user);
    }

    public List<UserResponse> searchUsers(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 100));
        return userRepository.searchByKeyword(
                kw,
                PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.ASC, "username")))
                .stream()
                .map(userResponseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "username"));
        User before = new User();
        before.setEmail(user.getEmail());
        before.setFullname(user.getFullname());
        before.setPhone(user.getPhone());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!user.getEmail().equals(request.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email is already in use", "EMAIL_TAKEN", "email");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFullname() != null) {
            user.setFullname(request.getFullname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updatedUser = userRepository.save(user);

        StringBuilder details = new StringBuilder();
        details.append("User self-updated profile. ");
        if (!equals(before.getEmail(), updatedUser.getEmail())) {
            details.append(String.format("email: '%s' -> '%s'; ", before.getEmail(), updatedUser.getEmail()));
        }
        if (!equals(before.getFullname(), updatedUser.getFullname())) {
            details.append(String.format("fullname: '%s' -> '%s'; ", before.getFullname(), updatedUser.getFullname()));
        }
        if (!equals(before.getPhone(), updatedUser.getPhone())) {
            details.append(String.format("phone: '%s' -> '%s'; ", before.getPhone(), updatedUser.getPhone()));
        }

        Long actorId = updatedUser.getId();
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();
        auditLogService.log(actorId, updatedUser.getId(), AuditAction.UPDATE_PROFILE, details.toString(), ip, userAgent);
        return userResponseMapper.toResponse(updatedUser);
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
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty", "FILE_EMPTY", "file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid avatar type. Allowed types: " + ALLOWED_AVATAR_TYPES,
                    "INVALID_AVATAR_TYPE",
                    "file");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException(
                    "Avatar size exceeds maximum allowed size of 5MB",
                    "AVATAR_SIZE_EXCEEDED",
                    "file");
        }

        String originalFileName = file.getOriginalFilename();
        String normalizedFileName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        boolean validExtension = ALLOWED_AVATAR_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
        if (!validExtension) {
            throw new BadRequestException(
                    "Invalid avatar extension. Allowed extensions: " + ALLOWED_AVATAR_EXTENSIONS,
                    "INVALID_AVATAR_EXTENSION",
                    "file");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!isSupportedImage(bytes)) {
                throw new BadRequestException(
                        "Uploaded file is not a valid image",
                        "INVALID_AVATAR_CONTENT",
                        "file");
            }
        } catch (IOException e) {
            throw new BadRequestException(
                    "Could not read uploaded avatar",
                    "INVALID_AVATAR_CONTENT",
                    "file");
        }
    }

    private String buildAvatarFolder(Long userId) {
        return storageProperties.getAvatarsFolder() + userId + "/";
    }

    private boolean isSupportedImage(byte[] bytes) {
        return isJpeg(bytes) || isPng(bytes) || isGif(bytes) || isWebp(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 0x47
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x38
                && (bytes[4] == 0x37 || bytes[4] == 0x39)
                && bytes[5] == 0x61;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50;
    }
}
