package com.studydocs.manager.controller.auth;

import com.studydocs.manager.application.user.UserApplicationService;
import com.studydocs.manager.dto.auth.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Change Password", description = "API for changing user password. Requires authentication. User must provide current password and new password.")
public class PasswordController {

    private final UserApplicationService userApplicationService;

    public PasswordController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userApplicationService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully.");
    }
}