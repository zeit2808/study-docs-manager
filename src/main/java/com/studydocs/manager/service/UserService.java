package com.studydocs.manager.service;
import com.studydocs.manager.dto.ProfileUpdateRequest;
import com.studydocs.manager.search.UserSearchService;
import com.studydocs.manager.dto.UserResponse;
import com.studydocs.manager.dto.UserUpdateRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired(required = false)
    private UserSearchService userSearchService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private SecurityUtils securityUtils;
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    @Cacheable(cacheNames = "usersById", key = "#id")
    public UserResponse getUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToResponse(user);
    }
    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User before = new User();
        before.setEmail(user.getEmail());
        before.setFullname(user.getFullname());
        before.setPhone(user.getPhone());
        before.setEnabled(user.getEnabled());

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty()){
            if (!user.getEmail().equals(updateRequest.getEmail()) &&
            userRepository.existsByEmail(updateRequest.getEmail())){
                throw new RuntimeException("Email is already in use");
            }
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPassword() != null && !updateRequest
                .getPassword().isEmpty()){
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
        if (updateRequest.getRoles()!=null && !updateRequest.getRoles()
                .isEmpty()){
            Set<Role> roles = new HashSet<>();
            updateRequest.getRoles().forEach(roleName -> {
                 Role role = roleRepository.findByName(roleName.toUpperCase())
                         .orElseThrow(()->new RuntimeException("Role "+roleName+" not found"));
                    roles.add(role);
            });
            user.setRoles(roles);
        }
        User updatedUser = userRepository.save(user);
        if (userSearchService != null) {
            userSearchService.indexUser(updatedUser);
        }

        // Build details
        StringBuilder details = new StringBuilder();
        details.append("Admin updated user ").append(id).append(". ");

        if (!equals(before.getEmail(), updatedUser.getEmail())) {
            details.append(String.format("email: '%s' -> '%s'; ",
                    before.getEmail(), updatedUser.getEmail()));
        }
        if (!equals(before.getFullname(), updatedUser.getFullname())) {
            details.append(String.format("fullname: '%s' -> '%s'; ",
                    before.getFullname(), updatedUser.getFullname()));
        }
        if (!equals(before.getPhone(), updatedUser.getPhone())) {
            details.append(String.format("phone: '%s' -> '%s'; ",
                    before.getPhone(), updatedUser.getPhone()));
        }
        if (!equals(before.getEnabled(), updatedUser.getEnabled())) {
            details.append(String.format("enabled: %s -> %s; ",
                    before.getEnabled(), updatedUser.getEnabled()));
        }

        Long actorId = securityUtils.getCurrentUserId(); // lấy từ SecurityContext
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();

        auditLogService.log(actorId, updatedUser.getId(),
                "UPDATE_USER", details.toString(), ip, userAgent);

        return convertToResponse(updatedUser);
    }
    private boolean equals(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public void deleteUser(Long id){
        if (!userRepository.existsById(id)){
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
        if (userSearchService != null) {
            userSearchService.deleteFromIndex(id);
        }
        Long actorId = securityUtils.getCurrentUserId();
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();

        auditLogService.log(actorId, id,
                "DELETE_USER", "Deleted user with id " + id, ip, userAgent);
    }
    @Transactional
    @Cacheable(cacheNames = "usersByUsername", key = "#username")
    public UserResponse getUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToResponse(user);
    }
    private UserResponse convertToResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsename(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullname(user.getFullname());
        response.setPhone(user.getPhone());
        response.setEnabled(user.getEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdateAt(user.getUpdateAt());
        response.setAvatarUrl(user.getAvatarUrl());
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        response.setRoles(roles);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public UserResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User before = new User();
        before.setEmail(user.getEmail());
        before.setFullname(user.getFullname());
        before.setPhone(user.getPhone());

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!user.getEmail().equals(request.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email is already in use");
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
        if (userSearchService != null) {
            userSearchService.indexUser(updatedUser);
        }

        StringBuilder details = new StringBuilder();
        details.append("User self-updated profile. ");

        if (!equals(before.getEmail(), updatedUser.getEmail())) {
            details.append(String.format("email: '%s' -> '%s'; ",
                    before.getEmail(), updatedUser.getEmail()));
        }
        if (!equals(before.getFullname(), updatedUser.getFullname())) {
            details.append(String.format("fullname: '%s' -> '%s'; ",
                    before.getFullname(), updatedUser.getFullname()));
        }
        if (!equals(before.getPhone(), updatedUser.getPhone())) {
            details.append(String.format("phone: '%s' -> '%s'; ",
                    before.getPhone(), updatedUser.getPhone()));
        }

        Long actorId = updatedUser.getId(); // tự sửa chính mình
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();

        auditLogService.log(actorId, updatedUser.getId(),
                "UPDATE_PROFILE", details.toString(), ip, userAgent);
        return convertToResponse(updatedUser);
    }

    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public UserResponse updateAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Xóa avatar cũ nếu có
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                fileStorageService.deleteAvatar(user.getAvatarUrl());
            }

            // Lưu avatar mới
            String avatarUrl = fileStorageService.storeAvatar(file, user.getId());
            user.setAvatarUrl(avatarUrl);

            User updatedUser = userRepository.save(user);
            if (userSearchService != null) {
                userSearchService.indexUser(updatedUser);
            }
            return convertToResponse(updatedUser);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage(), e);
        }
    }
}

