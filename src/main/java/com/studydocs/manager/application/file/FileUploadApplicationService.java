package com.studydocs.manager.application.file;

import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.file.FileDeleteResponse;
import com.studydocs.manager.dto.file.FileMetadataSummary;
import com.studydocs.manager.dto.file.PresignedUploadRequest;
import com.studydocs.manager.dto.file.PresignedUploadResponse;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.service.file.FileValidationService;
import com.studydocs.manager.service.file.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploadApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadApplicationService.class);

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain");

    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt");

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

    private static final long MAX_DOCUMENT_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_IMAGE_FILE_SIZE = 10 * 1024 * 1024;   // 10MB

    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final MinIOProperties minIOProperties;
    private final FileValidationService fileValidationService;
    private final TikaMetadataService tikaMetadataService;

    public FileUploadApplicationService(
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            MinIOProperties minIOProperties,
            FileValidationService fileValidationService,
            TikaMetadataService tikaMetadataService) {
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.minIOProperties = minIOProperties;
        this.fileValidationService = fileValidationService;
        this.tikaMetadataService = tikaMetadataService;
    }

    /**
     * Generate Presigned Upload URL cho Client tải file trực tiếp lên Storage
     */
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) throws IOException {
        String folderType = request.getFolderType() != null ? request.getFolderType().toUpperCase() : "DOCUMENTS";
        String folder;
        long maxSize;
        List<String> allowedTypes;
        List<String> allowedExtensions;

        switch (folderType) {
            case "THUMBNAILS":
                folder = storageProperties.getThumbnailsFolder();
                maxSize = MAX_IMAGE_FILE_SIZE;
                allowedTypes = ALLOWED_IMAGE_TYPES;
                allowedExtensions = ALLOWED_IMAGE_EXTENSIONS;
                break;
            case "AVATARS":
                folder = storageProperties.getAvatarsFolder();
                maxSize = MAX_IMAGE_FILE_SIZE;
                allowedTypes = ALLOWED_IMAGE_TYPES;
                allowedExtensions = ALLOWED_IMAGE_EXTENSIONS;
                break;
            case "DOCUMENTS":
            default:
                folder = storageProperties.getDocumentsFolder();
                maxSize = MAX_DOCUMENT_FILE_SIZE;
                allowedTypes = ALLOWED_DOCUMENT_TYPES;
                allowedExtensions = ALLOWED_DOCUMENT_EXTENSIONS;
                break;
        }

        // Validate tham số upload
        fileValidationService.validateDirectUploadParams(
                request.getFileName(),
                request.getContentType(),
                request.getFileSize(),
                allowedTypes,
                allowedExtensions,
                maxSize,
                "file");

        String sanitizedFilename = sanitizeFileName(request.getFileName());
        String objectName = folder + UUID.randomUUID() + "_" + sanitizedFilename;
        int expiryMinutes = minIOProperties.getPresignedUploadExpiryMinutes();

        logger.info("Generating presigned upload URL: objectName={}, size={}, contentType={}, expiry={}m",
                objectName, request.getFileSize(), request.getContentType(), expiryMinutes);

        String uploadUrl = storageProvider.generatePresignedUploadUrl(
                objectName,
                request.getContentType(),
                expiryMinutes);

        return new PresignedUploadResponse(
                uploadUrl,
                objectName,
                sanitizedFilename,
                request.getFileSize(),
                request.getContentType(),
                expiryMinutes);
    }

    /**
     * Xóa file khỏi Storage
     */
    public FileDeleteResponse deleteFile(String objectName) throws IOException {
        logger.info("Deleting file from storage: {}", objectName);
        if (!storageProvider.fileExists(objectName)) {
            logger.warn("File not found for deletion: {}", objectName);
            throw new NotFoundException("File not found: " + objectName, "FILE_NOT_FOUND", "objectName");
        }

        storageProvider.deleteFile(objectName);
        logger.info("File deletion SUCCESS: {}", objectName);
        return new FileDeleteResponse(true, "File deleted successfully", objectName);
    }

    /**
     * Bóc tách metadata từ file đã upload trên MinIO (dành cho client muốn pre-fill thông tin)
     */
    public FileMetadataSummary extractMetadataFromStorage(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new BadRequestException("Object name cannot be empty", "OBJECT_NAME_EMPTY", "objectName");
        }
        if (!storageProvider.fileExists(objectName)) {
            throw new NotFoundException("File not found on storage: " + objectName, "FILE_NOT_FOUND", "objectName");
        }

        try (InputStream inputStream = storageProvider.downloadFileAsStream(objectName)) {
            String filename = sanitizeFileName(objectName);
            return tikaMetadataService.extractMetadataSummary(inputStream, filename);
        } catch (Exception e) {
            logger.warn("Could not extract metadata for {}: {}", objectName, e.getMessage());
            return new FileMetadataSummary();
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed_file";
        }
        // Loại bỏ đường dẫn thư mục và ký tự đặc biệt nguy hiểm
        String simpleName = fileName;
        if (simpleName.contains("\\")) {
            simpleName = simpleName.substring(simpleName.lastIndexOf('\\') + 1);
        }
        if (simpleName.contains("/")) {
            simpleName = simpleName.substring(simpleName.lastIndexOf('/') + 1);
        }
        String clean = simpleName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return clean.length() > 120 ? clean.substring(clean.length() - 120) : clean;
    }
}
