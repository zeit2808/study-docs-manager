package com.studydocs.manager.application.user.usecase;

import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.dto.user.UserUpdateRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import com.studydocs.manager.service.document.AuditLogService;
import com.studydocs.manager.service.user.UserResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAdminUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;
    private final UserResponseMapper userResponseMapper;

    public UserAdminUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoleRepository roleRepository,
            AuditLogService auditLogService,
            SecurityUtils securityUtils,
            UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "username"));
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

    private boolean equals(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
