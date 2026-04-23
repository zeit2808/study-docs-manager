package com.studydocs.manager.application.user.usecase;

import com.studydocs.manager.dto.auth.ChangePasswordRequest;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.UnauthorizedException;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public void execute(ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated", "USER_NOT_AUTHENTICATED", null);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect", "INVALID_CURRENT_PASSWORD", "currentPassword");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException(
                    "New password must be different from current password",
                    "SAME_PASSWORD",
                    "newPassword");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
