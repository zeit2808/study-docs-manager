package com.studydocs.manager.controller.auth;

import com.studydocs.manager.dto.auth.ChangePasswordRequest;
import com.studydocs.manager.service.auth.PasswordChangeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Change Password", description = "API for changing user password. Requires authentication. User must provide current password and new password.")
public class PasswordController {

    private final PasswordChangeService passwordChangeService;

    public PasswordController(PasswordChangeService passwordChangeService) {
        this.passwordChangeService = passwordChangeService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passwordChangeService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully.");
    }
}