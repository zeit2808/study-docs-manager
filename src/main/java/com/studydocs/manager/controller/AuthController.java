package com.studydocs.manager.controller;

import com.studydocs.manager.dto.*;
import com.studydocs.manager.service.AuthService;
import com.studydocs.manager.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication",description = "APIs for user authentication and authorization")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private PasswordResetService passwordResetService;
    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate user and return JWT token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest){
        try{
            JwtResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 401);
            body.put("error", "Unauthorized");
            body.put("message", "Username/password is incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
    }
    @PostMapping("/register")
    @Operation(summary = "User Registration", description = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest){
        try{
            var user = authService.register(registerRequest);
            UserResponse response = new UserResponse();
            response.setId(user.getId());
            response.setUsename(user.getUsername());
            response.setEmail(user.getEmail());
            response.setFullname(user.getFullname());
            response.setPhone(user.getPhone());
            response.setEnabled(user.getEnabled());
            response.setCreatedAt(user.getCreatedAt());
            response.setUpdateAt(user.getUpdateAt());
            response.setRoles(user.getRoles().stream()
                    .map(role -> "ROLE_" + role.getName())
                    .collect(java.util.stream.Collectors.toSet()));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Send OTP to user's email for password reset")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendOtp(request);
        return ResponseEntity.ok("If this email exists, an OTP has been sent.");
    }


    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Reset password using OTP sent to email")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok("Password has been reset successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
