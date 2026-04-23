package com.studydocs.manager.application.auth.usecase;

import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.LoginRequest;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.UnauthorizedException;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import com.studydocs.manager.service.auth.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginUseCase {

    private static final Logger logger = LoggerFactory.getLogger(LoginUseCase.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;

    public LoginUseCase(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtTokenProvider tokenProvider,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public JwtResponse execute(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException(
                        "Username/password is incorrect", "INVALID_CREDENTIALS", null));

        LocalDateTime unlockTime = loginAttemptService.isAccountLocked(request.getUsername());
        if (unlockTime != null) {
            throw new UnauthorizedException(
                    "Account is locked until " + unlockTime, "ACCOUNT_LOCKED", null);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            loginAttemptService.resetFailedAttempts(request.getUsername());

            String jwt = tokenProvider.generateToken(authentication);
            String roleName = "ROLE_" + user.getRole().getName();
            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roleName);

        } catch (org.springframework.security.core.AuthenticationException ex) {
            boolean isLocked = loginAttemptService.incrementFailedAttempts(request.getUsername());
            if (isLocked) {
                LocalDateTime locked = loginAttemptService.isAccountLocked(request.getUsername());
                logger.warn("Account locked due to multiple failed login attempts - username: {}, unlockTime: {}",
                        request.getUsername(), locked);
                throw new UnauthorizedException(
                        "Account is locked until " + locked + " due to 5 failed login attempts",
                        "ACCOUNT_LOCKED", null);
            }
            if (logger.isDebugEnabled()) {
                int attempts = loginAttemptService.getFailedAttempts(request.getUsername());
                logger.debug("Login failed - username: {}, current attempts: {}", request.getUsername(), attempts);
            }
            throw new UnauthorizedException("Username/password is incorrect", "INVALID_CREDENTIALS", null);
        }
    }
}
