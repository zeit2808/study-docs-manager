package com.studydocs.manager.controller.file;

import com.studydocs.manager.application.file.FileUploadApplicationService;
import com.studydocs.manager.dto.common.ErrorResponse;
import com.studydocs.manager.dto.file.FileDeleteResponse;
import com.studydocs.manager.dto.file.FileDownloadResult;
import com.studydocs.manager.dto.file.FileUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "APIs for uploading files to MinIO")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileUploadApplicationService fileUploadApplicationService;

    public FileUploadController(
            FileUploadApplicationService fileUploadApplicationService) {
        this.fileUploadApplicationService = fileUploadApplicationService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a document file", description = "Upload a single document file to MinIO storage. Optionally extracts metadata (title, page count, etc.) to pre-fill the document creation form.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @Parameter(description = "Document file to upload", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "Extract metadata from file using Apache Tika (default: true)") @RequestParam(value = "extractMetadata", defaultValue = "true") boolean extractMetadata)
            throws IOException {
        return ResponseEntity.ok(fileUploadApplicationService.uploadDocument(file, extractMetadata));
    }

    @PostMapping(value = "/upload-thumbnail", consumes = "multipart/form-data")
    @Operation(summary = "Upload thumbnail image", description = "Upload a single thumbnail image to storage. Returns file URL and metadata if successful.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileUploadResponse> uploadThumbnail(
            @Parameter(description = "Thumbnail image file to upload", required = true) @RequestPart("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(fileUploadApplicationService.uploadThumbnail(file));
    }

    @GetMapping("/download")
    @Operation(summary = "Download file from storage", description = "Download a file from storage by providing the object name . Returns the file content with appropriate headers or error message.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "Object name of the file to download", required = true) @RequestParam("objectName") String objectName)
            throws IOException {
        if (objectName == null || objectName.trim().isEmpty()) {
            ErrorResponse error = new ErrorResponse(400, "Bad Request", "Object name is required");
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        }

        FileDownloadResult result = fileUploadApplicationService.downloadFile(objectName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.resource());
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
