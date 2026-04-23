package com.studydocs.manager.application.auth;

import com.studydocs.manager.application.auth.usecase.ForgotPasswordUseCase;
import com.studydocs.manager.application.auth.usecase.LoginUseCase;
import com.studydocs.manager.application.auth.usecase.RegisterUseCase;
import com.studydocs.manager.application.auth.usecase.ResetPasswordUseCase;
import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.LoginRequest;
import com.studydocs.manager.dto.auth.RegisterRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.dto.user.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthApplicationService(
            LoginUseCase loginUseCase,
            RegisterUseCase registerUseCase,
            ForgotPasswordUseCase forgotPasswordUseCase,
            ResetPasswordUseCase resetPasswordUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    public JwtResponse login(LoginRequest request) {
        return loginUseCase.execute(request);
    }

    public UserResponse register(RegisterRequest request) {
        return registerUseCase.execute(request);
    }

    public void sendPasswordResetOtp(ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request);
    }

    public void resetPassword(ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request);
    }
}
