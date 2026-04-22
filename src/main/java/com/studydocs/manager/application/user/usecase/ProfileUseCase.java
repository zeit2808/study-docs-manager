package com.studydocs.manager.application.user.usecase;

import com.studydocs.manager.dto.user.ProfileUpdateRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import com.studydocs.manager.service.document.AuditLogService;
import com.studydocs.manager.service.user.UserResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProfileUseCase {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final UserResponseMapper userResponseMapper;

    public ProfileUseCase(
            UserRepository userRepository,
            AuditLogService auditLogService,
            SecurityUtils securityUtils,
            UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional
    public UserResponse getCurrentProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "username"));
        return userResponseMapper.toResponse(user);
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

    private boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
