package com.studydocs.manager.controller.file;

import com.studydocs.manager.application.file.FileManagerApplicationService;
import com.studydocs.manager.dto.filemanager.FileManagerDeleteRequest;
import com.studydocs.manager.dto.filemanager.FileManagerDeleteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerTransferRequest;
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
@RequestMapping("/api/file-manager")
@Tag(name = "File Manager", description = "APIs for deleting, moving, and copying folders/documents like a desktop file manager")
@SecurityRequirement(name = "bearerAuth")
public class FileManagerController {

    private final FileManagerApplicationService fileManagerApplicationService;

    public FileManagerController(FileManagerApplicationService fileManagerApplicationService) {
        this.fileManagerApplicationService = fileManagerApplicationService;
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy folders/documents", description = "Copy multiple folders/documents into a target folder or root")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileManagerPasteResponse> copy(@Valid @RequestBody FileManagerTransferRequest request) {
        return ResponseEntity.ok(fileManagerApplicationService.copy(request));
    }

    @PostMapping("/move")
    @Operation(summary = "Move folders/documents", description = "Move multiple folders/documents into a target folder or root")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileManagerPasteResponse> move(@Valid @RequestBody FileManagerTransferRequest request) {
        return ResponseEntity.ok(fileManagerApplicationService.move(request));
    }

    @PostMapping("/delete")
    @Operation(
            summary = "Delete selected folders/documents",
            description = "Soft-delete multiple folders and documents from the current level. Selected folders are deleted recursively together with their nested documents.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileManagerDeleteResponse> delete(@Valid @RequestBody FileManagerDeleteRequest request) {
        return ResponseEntity.ok(fileManagerApplicationService.delete(request));
    }
}
