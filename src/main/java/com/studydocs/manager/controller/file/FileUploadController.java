package com.studydocs.manager.controller.file;

import com.studydocs.manager.application.file.FileUploadApplicationService;
import com.studydocs.manager.dto.file.FileDeleteResponse;
import com.studydocs.manager.dto.file.PresignedUploadRequest;
import com.studydocs.manager.dto.file.PresignedUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "APIs for managing storage files and presigned upload URLs")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileUploadApplicationService fileUploadApplicationService;

    public FileUploadController(
            FileUploadApplicationService fileUploadApplicationService) {
        this.fileUploadApplicationService = fileUploadApplicationService;
    }

    @PostMapping("/presigned-upload-url")
    @Operation(summary = "Generate presigned upload URL", description = "Generate a presigned PUT URL allowing the client to upload a file directly to MinIO storage. Bypasses application server for high performance and low memory consumption.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PresignedUploadResponse> generatePresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) throws IOException {
        return ResponseEntity.ok(fileUploadApplicationService.generatePresignedUploadUrl(request));
    }

    @GetMapping("/metadata")
    @Operation(summary = "Extract metadata from uploaded file", description = "Extract metadata summary (title, page count, etc.) from an object already uploaded to MinIO storage.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<com.studydocs.manager.dto.file.FileMetadataSummary> extractMetadata(
            @Parameter(description = "Object name of the file in storage", required = true) @RequestParam("objectName") String objectName) {
        return ResponseEntity.ok(fileUploadApplicationService.extractMetadataFromStorage(objectName));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file from storage", description = "Delete a file from storage by providing the object name. Returns confirmation of deletion.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileDeleteResponse> deleteFile(
            @Parameter(description = "Object name of the file to delete", required = true) @RequestParam("objectName") String objectName)
            throws IOException {
        return ResponseEntity.ok(fileUploadApplicationService.deleteFile(objectName));
    }
}
