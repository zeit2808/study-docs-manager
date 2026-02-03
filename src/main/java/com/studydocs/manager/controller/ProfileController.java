package com.studydocs.manager.controller;

import com.studydocs.manager.dto.ProfileUpdateRequest;
import com.studydocs.manager.dto.UserResponse;
import com.studydocs.manager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "User Profile", description = "APIs for current user profile")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get current user profile")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> getCurrentProfile(Authentication authentication) {
        String username = authentication.getName();
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping
    @Operation(summary = "Update current user profile")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> updateCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {

        String username = authentication.getName();
        UserResponse updated = userService.updateProfile(username, request);
        return ResponseEntity.ok(updated);
    }
    @PostMapping("/avatar")
    @Operation(summary = "Upload avatar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        String username = authentication.getName();
        UserResponse updated = userService.updateAvatar(username, file);
        return ResponseEntity.ok(updated);
    }
}