package com.studydocs.manager.controller;

import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.BatchUploadResponse;
import com.studydocs.manager.dto.FileMetadata;
import com.studydocs.manager.dto.FileUploadResponse;
import com.studydocs.manager.dto.FileUploadResult;
import com.studydocs.manager.service.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * FileUploadController - Controller xử lý upload file lên MinIO
 * 
 * Giải thích chi tiết từng phần:
 * 1. ENDPOINTS: /api/files/upload (documents), /api/files/upload-thumbnail
 * (images)
 * 2. AUTHENTICATION: Yêu cầu JWT token với role USER hoặc ADMIN
 * 3. VALIDATION: Kiểm tra file size, MIME type trước khi upload
 * 4. STORAGE: Upload lên MinIO Server, trả về URL để truy cập
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "File Upload", description = "APIs for uploading files to MinIO")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    // Các loại file tài liệu được phép
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain");

    // Các loại file ảnh được phép
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    // 50MB limit
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Autowired
    private StorageProvider storageProvider;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private TikaMetadataService tikaMetadataService;

    /**
     * Upload tài liệu (PDF, Word, Excel, PPT, etc.)
     * 
     * Flow: Client gửi file → Validate → Upload MinIO → Extract metadata (optional)
     * → Trả URL + metadata
     * 
     * Two-step upload pattern:
     * Step 1: Upload file and get metadata
     * Step 2: Frontend uses metadata to pre-fill form, user reviews, then calls
     * POST /documents
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload document files (batch)", description = "Upload up to 5 document files to MinIO storage. Processes each file independently - partial failures are allowed. Optionally extracts metadata for use in document creation form.")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadDocument(
            @Parameter(description = "Document files to upload (max 5 files)", required = true) @RequestPart("files") MultipartFile[] files,

            @Parameter(description = "Extract metadata from files using Apache Tika (default: true)") @RequestParam(value = "extractMetadata", defaultValue = "true") boolean extractMetadata) {

        try {
            // Validation: Check file count limit
            if (files == null || files.length == 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("At least one file is required");
            }
            if (files.length > 5) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Cannot upload more than 5 files at once. You sent " + files.length + " files.");
            }

            logger.info("Batch uploading {} documents, extractMetadata: {}", files.length, extractMetadata);

            // Process each file independently (partial success allowed)
            List<FileUploadResult> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String originalFileName = file.getOriginalFilename();

                try {
                    // Step 1: Validate file
                    validateFile(file, ALLOWED_DOCUMENT_TYPES);

                    logger.debug("[{}/{}] Processing: {}, size: {}",
                            i + 1, files.length, originalFileName, file.getSize());

                    // Step 2: Extract metadata if requested
                    com.studydocs.manager.dto.FileMetadataSummary metadata = null;
                    if (extractMetadata) {
                        try {
                            metadata = tikaMetadataService.extractMetadataSummary(file);
                            logger.debug("Metadata extracted - Title: {}, Pages: {}",
                                    metadata.getTitle(), metadata.getPageCount());
                        } catch (Exception e) {
                            // Don't fail upload if metadata extraction fails
                            logger.warn("Failed to extract metadata from {}: {}",
                                    originalFileName, e.getMessage());
                        }
                    }

                    // Step 3: Upload file to storage
                    String fileUrl = storageProvider.uploadFile(file, storageProperties.getDocumentsFolder());

                    // Step 4: Build response for this file
                    FileUploadResponse response = new FileUploadResponse();
                    response.setFileUrl(fileUrl);
                    response.setFileName(originalFileName);
                    response.setFileSize(file.getSize());
                    response.setFileType(file.getContentType());

                    // Extract objectName from fileUrl
                    // Note: This logic works for MinIO URLs. For other providers, might need
                    // adjustment
                    String objectName = storageProperties.getDocumentsFolder() +
                            fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                    response.setObjectName(objectName);
                    response.setMetadata(metadata);

                    // Success result
                    results.add(new FileUploadResult(originalFileName, i, response));
                    successCount++;
                    logger.info("[{}/{}] Upload SUCCESS: {}", i + 1, files.length, originalFileName);

                } catch (Exception e) {
                    // Failure result - don't break, continue with other files
                    String errorMessage = e.getMessage();
                    results.add(new FileUploadResult(originalFileName, i, errorMessage));
                    failureCount++;
                    logger.warn("[{}/{}] Upload FAILED: {} - Error: {}",
                            i + 1, files.length, originalFileName, errorMessage);
                }
            }

            // Build final batch response
            BatchUploadResponse batchResponse = new BatchUploadResponse(
                    files.length, successCount, failureCount, results);

            logger.info("Batch upload completed: {} total, {} success, {} failed",
                    files.length, successCount, failureCount);

            return ResponseEntity.ok(batchResponse);

        } catch (Exception e) {
            logger.error("Unexpected error in batch upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error during batch upload: " + e.getMessage());
        }
    }

    /**
     * Upload single thumbnail image (PNG, JPG, GIF, WebP)
     * 
     * Flow: Client uploads 1 thumbnail → Validate → Upload storage → Return URL
     * 
     * Note: Single upload prevents confusion with document-thumbnail mapping.
     * Frontend uploads 1 thumbnail per document form.
     * 
     * @param file Thumbnail image file
     * @return FileUploadResponse with file URL and metadata
     */
    @PostMapping(value = "/upload-thumbnail", consumes = "multipart/form-data")
    @Operation(summary = "Upload thumbnail image", description = "Upload a single thumbnail image to storage. Returns file URL and metadata if successful.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadThumbnail(
            @Parameter(description = "Thumbnail image file to upload", required = true) @RequestPart("file") MultipartFile file) {

        try {
            String originalFileName = file.getOriginalFilename();
            logger.info("Thumbnail upload started: {}, size: {}", originalFileName, file.getSize());

            // Step 1: Validate file
            validateFile(file, ALLOWED_IMAGE_TYPES);

            // Step 2: Upload thumbnail to storage
            String fileUrl = storageProvider.uploadFile(file, storageProperties.getThumbnailsFolder());

            // Step 3: Build response
            FileUploadResponse response = new FileUploadResponse();
            response.setFileUrl(fileUrl);
            response.setFileName(originalFileName);
            response.setFileSize(file.getSize());
            response.setFileType(file.getContentType());

            // Extract objectName from fileUrl
            String objectName = storageProperties.getThumbnailsFolder() +
                    fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            response.setObjectName(objectName);

            logger.info("Thumbnail upload SUCCESS: {}", originalFileName);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Thumbnail upload FAILED: {} - Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Validate file trước khi upload
     * 
     * Kiểm tra: File rỗng, MIME type, Kích thước
     */
    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + allowedTypes);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 50MB");
        }
    }
}
