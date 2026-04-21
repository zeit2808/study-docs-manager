package com.studydocs.manager.controller.file;

import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.common.ErrorResponse;
import com.studydocs.manager.dto.file.FileDeleteResponse;
import com.studydocs.manager.dto.file.FileUploadResponse;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.service.file.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import com.studydocs.manager.storage.StoredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "APIs for uploading files to MinIO")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain");

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp");

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final TikaMetadataService tikaMetadataService;

    public FileUploadController(
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            TikaMetadataService tikaMetadataService) {
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.tikaMetadataService = tikaMetadataService;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a document file", description = "Upload a single document file to MinIO storage. Optionally extracts metadata (title, page count, etc.) to pre-fill the document creation form.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @Parameter(description = "Document file to upload", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "Extract metadata from file using Apache Tika (default: true)") @RequestParam(value = "extractMetadata", defaultValue = "true") boolean extractMetadata)
            throws IOException {

        String originalFileName = file.getOriginalFilename();
        validateFile(file, ALLOWED_DOCUMENT_TYPES);
        logger.info("Uploading document: {}, size: {}, extractMetadata: {}", originalFileName, file.getSize(),
                extractMetadata);

        com.studydocs.manager.dto.file.FileMetadataSummary metadata = null;
        if (extractMetadata) {
            try {
                metadata = tikaMetadataService.extractMetadataSummary(file);
                logger.debug("Metadata extracted - Title: {}, Pages: {}", metadata.getTitle(), metadata.getPageCount());
            } catch (Exception e) {
                logger.warn("Failed to extract metadata from {}: {} (Type: {})",
                        originalFileName, e.getMessage(), e.getClass().getSimpleName());
            }
        }

        StoredFile storedFile = storageProvider.uploadFile(file, storageProperties.getDocumentsFolder());

        FileUploadResponse response = new FileUploadResponse();
        response.setFileUrl(storedFile.fileUrl());
        response.setFileName(originalFileName);
        response.setFileSize(file.getSize());
        response.setFileType(file.getContentType());
        response.setObjectName(storedFile.objectName());
        response.setMetadata(metadata);

        logger.info("Document upload SUCCESS: {}", originalFileName);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload-thumbnail", consumes = "multipart/form-data")
    @Operation(summary = "Upload thumbnail image", description = "Upload a single thumbnail image to storage. Returns file URL and metadata if successful.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileUploadResponse> uploadThumbnail(
            @Parameter(description = "Thumbnail image file to upload", required = true) @RequestPart("file") MultipartFile file)
            throws IOException {

        String originalFileName = file.getOriginalFilename();
        logger.info("Thumbnail upload started: {}, size: {}", originalFileName, file.getSize());
        validateFile(file, ALLOWED_IMAGE_TYPES);

        StoredFile storedFile = storageProvider.uploadFile(file, storageProperties.getThumbnailsFolder());

        FileUploadResponse response = new FileUploadResponse();
        response.setFileUrl(storedFile.fileUrl());
        response.setFileName(originalFileName);
        response.setFileSize(file.getSize());
        response.setFileType(file.getContentType());
        response.setObjectName(storedFile.objectName());

        logger.info("Thumbnail upload SUCCESS: {}", originalFileName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    @Operation(summary = "Download file from storage", description = "Download a file from storage by providing the object name . Returns the file content with appropriate headers or error message.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> downloadFile(
            @Parameter(description = "Object name of the file to download", required = true) @RequestParam("objectName") String objectName)
            throws IOException {

        logger.info("Downloading file: {}", objectName);

        if (objectName == null || objectName.trim().isEmpty()) {
            ErrorResponse error = new ErrorResponse(400, "Bad Request", "Object name is required");
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error);
        }

        String normalizedObjectName = URLDecoder.decode(objectName, StandardCharsets.UTF_8).replaceFirst("^/", "");
        if (!storageProvider.fileExists(normalizedObjectName)) {
            logger.warn("File not found: {}", normalizedObjectName);
            throw new NotFoundException("File not found: " + normalizedObjectName, "FILE_NOT_FOUND", "objectName");
        }

        InputStream fileStream = storageProvider.downloadFileAsStream(normalizedObjectName);
        Resource resource = new InputStreamResource(fileStream);
        String filename = extractFilename(normalizedObjectName);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        logger.info("File download SUCCESS: {}", normalizedObjectName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete file from storage", description = "Delete a file from storage by providing the object name. Returns confirmation of deletion.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<FileDeleteResponse> deleteFile(
            @Parameter(description = "Object name of the file to delete", required = true) @RequestParam("objectName") String objectName)
            throws IOException {

        logger.info("Deleting file: {}", objectName);

        if (!storageProvider.fileExists(objectName)) {
            logger.warn("File not found for deletion: {}", objectName);
            throw new NotFoundException("File not found: " + objectName, "FILE_NOT_FOUND", "objectName");
        }

        storageProvider.deleteFile(objectName);
        FileDeleteResponse response = new FileDeleteResponse(true, "File deleted successfully", objectName);
        logger.info("File deletion SUCCESS: {}", objectName);
        return ResponseEntity.ok(response);
    }

    private String extractFilename(String objectName) {
        String filename = objectName;
        if (objectName.contains("/")) {
            filename = objectName.substring(objectName.lastIndexOf("/") + 1);
        }
        if (filename.contains("_")) {
            int firstUnderscore = filename.indexOf("_");
            filename = filename.substring(firstUnderscore + 1);
        }
        return filename;
    }

    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty", "FILE_EMPTY", "file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Allowed types: " + allowedTypes, "INVALID_FILE_TYPE", "file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 50MB", "FILE_SIZE_EXCEEDED", "file");
        }

        if (allowedTypes == ALLOWED_IMAGE_TYPES) {
            validateImageContent(file);
        }
    }

    private void validateImageContent(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String normalizedFileName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        boolean validExtension = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
        if (!validExtension) {
            throw new BadRequestException(
                    "Invalid image extension. Allowed extensions: " + ALLOWED_IMAGE_EXTENSIONS,
                    "INVALID_IMAGE_EXTENSION",
                    "file");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new BadRequestException("File cannot be empty", "FILE_EMPTY", "file");
            }

            if (!isSupportedImage(bytes)) {
                throw new BadRequestException(
                        "Uploaded file is not a valid image",
                        "INVALID_IMAGE_CONTENT",
                        "file");
            }
        } catch (IOException e) {
            throw new BadRequestException(
                    "Could not read uploaded image",
                    "INVALID_IMAGE_CONTENT",
                    "file");
        }
    }

    private boolean isSupportedImage(byte[] bytes) {
        return isJpeg(bytes) || isPng(bytes) || isGif(bytes) || isWebp(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 0x47
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x38
                && (bytes[4] == 0x37 || bytes[4] == 0x39)
                && bytes[5] == 0x61;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50;
    }
}
