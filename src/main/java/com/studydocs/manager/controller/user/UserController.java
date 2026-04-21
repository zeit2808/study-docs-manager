package com.studydocs.manager.controller.user;

import com.studydocs.manager.application.user.UserApplicationService;
import com.studydocs.manager.dto.auth.AdminRegisterRequest;
import com.studydocs.manager.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    @PostMapping
    @Operation(summary = "Register User by Admin", description = "Admin can register a new user with any role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> registerUserByAdmin(
            @Valid @RequestBody AdminRegisterRequest adminRegisterRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userApplicationService.registerUserByAdmin(adminRegisterRequest));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by username, fullname or email using database query")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(userApplicationService.searchUsers(keyword, limit));
    }

    @GetMapping
    @Operation(summary = "Get All Users", description = "Retrieve a list of all registered users")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userApplicationService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Retrieve user details by user")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse user = userApplicationService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get Users By Username", description = "Admin and User can access")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userApplicationService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update User Information", description = "Only ADMIN can update user information. Regular users should use /api/profile endpoint to update their own profile.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody com.studydocs.manager.dto.user.UserUpdateRequest updateRequest) {
        UserResponse updatedUser = userApplicationService.updateUser(id, updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Admin can delete a user by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userApplicationService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
