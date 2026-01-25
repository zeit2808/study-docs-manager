package com.studydocs.manager.controller;

import com.studydocs.manager.dto.JwtResponse;
import com.studydocs.manager.dto.LoginRequest;
import com.studydocs.manager.dto.RegisterRequest;
import com.studydocs.manager.dto.UserResponse;
import com.studydocs.manager.service.AuthService;
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

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication",description = "APIs for user authentication and authorization")
public class AuthController {
    @Autowired
    private AuthService authService;
    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticate user and return JWT token")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        try{
            JwtResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
}
