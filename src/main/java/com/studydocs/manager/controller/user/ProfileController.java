package com.studydocs.manager.controller.user;

import com.studydocs.manager.application.user.UserApplicationService;
import com.studydocs.manager.dto.user.ProfileUpdateRequest;
import com.studydocs.manager.dto.user.UserResponse;
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

    private final UserApplicationService userApplicationService;

    public ProfileController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @GetMapping
    @Operation(summary = "Get current user profile")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> getCurrentProfile(Authentication authentication) {
        String username = authentication.getName();
        UserResponse user = userApplicationService.getCurrentProfile(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping
    @Operation(summary = "Update current user profile")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> updateCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {

        String username = authentication.getName();
        UserResponse updated = userApplicationService.updateCurrentProfile(username, request);
        return ResponseEntity.ok(updated);
    }
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Upload avatar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        String username = authentication.getName();
        UserResponse updated = userApplicationService.updateAvatar(username, file);
        return ResponseEntity.ok(updated);
    }
}
