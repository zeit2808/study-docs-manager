package com.studydocs.manager.application.user;

import com.studydocs.manager.dto.auth.AdminRegisterRequest;
import com.studydocs.manager.dto.user.ProfileUpdateRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.dto.user.UserUpdateRequest;
import com.studydocs.manager.service.auth.AuthService;
import com.studydocs.manager.service.user.UserResponseMapper;
import com.studydocs.manager.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserApplicationService {

    private final UserService userService;
    private final AuthService authService;
    private final UserResponseMapper userResponseMapper;

    public UserApplicationService(
            UserService userService,
            AuthService authService,
            UserResponseMapper userResponseMapper) {
        this.userService = userService;
        this.authService = authService;
        this.userResponseMapper = userResponseMapper;
    }

    public UserResponse registerUserByAdmin(AdminRegisterRequest request) {
        return userResponseMapper.toResponse(authService.registerByAdmin(request));
    }

    public List<UserResponse> searchUsers(String keyword, int limit) {
        return userService.searchUsers(keyword, limit);
    }

    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    public UserResponse getUserById(Long id) {
        return userService.getUserById(id);
    }

    public UserResponse getUserByUsername(String username) {
        return userService.getUserByUsername(username);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        return userService.updateUser(id, request);
    }

    public void deleteUser(Long id) {
        userService.deleteUser(id);
    }

    public UserResponse getCurrentProfile(String username) {
        return userService.getUserByUsername(username);
    }

    public UserResponse updateCurrentProfile(String username, ProfileUpdateRequest request) {
        return userService.updateProfile(username, request);
    }

    public UserResponse updateAvatar(String username, MultipartFile file) {
        return userService.updateAvatar(username, file);
    }
}
