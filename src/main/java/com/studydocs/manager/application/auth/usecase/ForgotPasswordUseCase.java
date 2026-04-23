package com.studydocs.manager.application.auth.usecase;

import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.service.auth.PasswordResetService;
import org.springframework.stereotype.Service;

/**
 * Orchestrates "forgot password" flow: validate email → generate OTP → send email.
 * Email infrastructure (MIME template, JavaMailSender) is encapsulated in PasswordResetService.
 */
@Service
public class ForgotPasswordUseCase {

    private final PasswordResetService passwordResetService;

    public ForgotPasswordUseCase(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    public void execute(ForgotPasswordRequest request) {
        passwordResetService.sendOtp(request);
    }
}
