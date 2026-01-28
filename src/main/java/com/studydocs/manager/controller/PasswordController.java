package com.studydocs.manager.controller;

import com.studydocs.manager.dto.ChangePasswordRequest;
import com.studydocs.manager.service.PasswordChangeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
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