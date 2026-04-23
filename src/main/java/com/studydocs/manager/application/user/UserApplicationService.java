package com.studydocs.manager.application.user;

import com.studydocs.manager.application.user.usecase.AvatarUseCase;
import com.studydocs.manager.application.user.usecase.ChangePasswordUseCase;
import com.studydocs.manager.application.user.usecase.ProfileUseCase;
import com.studydocs.manager.application.user.usecase.RegisterByAdminUseCase;
import com.studydocs.manager.application.user.usecase.UserAdminUseCase;
import com.studydocs.manager.dto.auth.AdminRegisterRequest;
import com.studydocs.manager.dto.auth.ChangePasswordRequest;
import com.studydocs.manager.dto.user.ProfileUpdateRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.dto.user.UserUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserApplicationService {

    private final UserAdminUseCase userAdminUseCase;
    private final ProfileUseCase profileUseCase;
    private final AvatarUseCase avatarUseCase;
    private final RegisterByAdminUseCase registerByAdminUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    public UserApplicationService(
            UserAdminUseCase userAdminUseCase,
            ProfileUseCase profileUseCase,
            AvatarUseCase avatarUseCase,
            RegisterByAdminUseCase registerByAdminUseCase,
            ChangePasswordUseCase changePasswordUseCase) {
        this.userAdminUseCase = userAdminUseCase;
        this.profileUseCase = profileUseCase;
        this.avatarUseCase = avatarUseCase;
        this.registerByAdminUseCase = registerByAdminUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    public UserResponse registerUserByAdmin(AdminRegisterRequest request) {
        return registerByAdminUseCase.execute(request);
    }

    public List<UserResponse> searchUsers(String keyword, int limit) {
        return userAdminUseCase.searchUsers(keyword, limit);
    }

    public List<UserResponse> getAllUsers() {
        return userAdminUseCase.getAllUsers();
    }

    public UserResponse getUserById(Long id) {
        return userAdminUseCase.getUserById(id);
    }

    public UserResponse getUserByUsername(String username) {
        return userAdminUseCase.getUserByUsername(username);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        return userAdminUseCase.updateUser(id, request);
    }

    public void deleteUser(Long id) {
        userAdminUseCase.deleteUser(id);
    }

    public UserResponse getCurrentProfile(String username) {
        return profileUseCase.getCurrentProfile(username);
    }

    public UserResponse updateCurrentProfile(String username, ProfileUpdateRequest request) {
        return profileUseCase.updateProfile(username, request);
    }

    public UserResponse updateAvatar(String username, MultipartFile file) {
        return avatarUseCase.updateAvatar(username, file);
    }

    public void changePassword(ChangePasswordRequest request) {
        changePasswordUseCase.execute(request);
    }
}
