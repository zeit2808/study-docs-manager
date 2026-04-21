package com.studydocs.manager.application.auth;

import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.LoginRequest;
import com.studydocs.manager.dto.auth.RegisterRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.service.auth.AuthService;
import com.studydocs.manager.service.auth.PasswordResetService;
import com.studydocs.manager.service.user.UserResponseMapper;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UserResponseMapper userResponseMapper;

    public AuthApplicationService(
            AuthService authService,
            PasswordResetService passwordResetService,
            UserResponseMapper userResponseMapper) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.userResponseMapper = userResponseMapper;
    }

    public JwtResponse login(LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    public UserResponse register(RegisterRequest registerRequest) {
        return userResponseMapper.toResponse(authService.register(registerRequest));
    }

    public void sendPasswordResetOtp(ForgotPasswordRequest request) {
        passwordResetService.sendOtp(request);
    }

    public void resetPassword(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }
}
