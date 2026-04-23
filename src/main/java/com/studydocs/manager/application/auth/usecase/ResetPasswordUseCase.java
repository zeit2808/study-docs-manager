package com.studydocs.manager.application.auth.usecase;

import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.service.auth.PasswordResetService;
import org.springframework.stereotype.Service;

/**
 * Orchestrates password reset flow: validate OTP → update password → mark token used.
 * Business rules (expiry, OTP match) are encapsulated in PasswordResetService.
 */
@Service
public class ResetPasswordUseCase {

    private final PasswordResetService passwordResetService;

    public ResetPasswordUseCase(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    public void execute(ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }
}
