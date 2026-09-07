package com.studydocs.manager.application.auth.usecase;

import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import com.studydocs.manager.service.auth.PasswordResetService;
import org.springframework.stereotype.Service;

/**
 * Orchestrates password reset flow: validate OTP → update password → clear old reset tokens → issue new JWT.
 * Business rules (expiry, OTP match) are encapsulated in PasswordResetService.
 */
@Service
public class ResetPasswordUseCase {

    private final PasswordResetService passwordResetService;
    private final JwtTokenProvider tokenProvider;

    public ResetPasswordUseCase(
            PasswordResetService passwordResetService,
            JwtTokenProvider tokenProvider
    ) {
        this.passwordResetService = passwordResetService;
        this.tokenProvider = tokenProvider;
    }

    public JwtResponse execute(ResetPasswordRequest request) {
        User user = passwordResetService.resetPassword(request);
        String roleName = "ROLE_" + user.getRole().getName();
        String jwt = tokenProvider.generateToken(
                user.getUsername(),
                roleName,
                user.getTokenVersion()
        );
        return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roleName);
    }
}

