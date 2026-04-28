package com.studydocs.manager.controller.admin;

import com.studydocs.manager.application.admin.AdminTrashCleanupApplicationService;
import com.studydocs.manager.dto.admin.AdminTrashPurgeRequest;
import com.studydocs.manager.dto.admin.AdminTrashPurgeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trash")
@Tag(name = "Admin Trash Cleanup", description = "Admin-only APIs for manual trash purge across documents and folders")
@SecurityRequirement(name = "bearerAuth")
public class AdminTrashController {

    private final AdminTrashCleanupApplicationService adminTrashCleanupApplicationService;

    public AdminTrashController(AdminTrashCleanupApplicationService adminTrashCleanupApplicationService) {
        this.adminTrashCleanupApplicationService = adminTrashCleanupApplicationService;
    }

    @PostMapping("/purge")
    @Operation(summary = "Purge trash manually", description = "Admin-only manual purge for deleted documents, deleted folder trees, or both. Stored files are deleted immediately.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminTrashPurgeResponse> purgeTrash(@Valid @RequestBody AdminTrashPurgeRequest request) {
        return ResponseEntity.ok(adminTrashCleanupApplicationService.purge(request));
    }
}
