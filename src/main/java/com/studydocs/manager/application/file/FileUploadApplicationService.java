package com.studydocs.manager.application.file;

import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.file.FileDeleteResponse;
import com.studydocs.manager.dto.file.FileDownloadResult;
import com.studydocs.manager.dto.file.FileMetadataSummary;
import com.studydocs.manager.dto.file.FileUploadResponse;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.service.file.FileValidationService;
import com.studydocs.manager.service.file.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import com.studydocs.manager.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
    private final FileValidationService fileValidationService;

    public FileUploadApplicationService(
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            TikaMetadataService tikaMetadataService,
            FileValidationService fileValidationService) {
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.tikaMetadataService = tikaMetadataService;
        this.fileValidationService = fileValidationService;
    }

    public FileUploadResponse uploadDocument(MultipartFile file, boolean extractMetadata) throws IOException {
        String originalFileName = file.getOriginalFilename();
        fileValidationService.validateContentTypeAndSize(
                file,
                ALLOWED_DOCUMENT_TYPES,
                MAX_FILE_SIZE,
                "Invalid file type. Allowed types: " + ALLOWED_DOCUMENT_TYPES,
                "INVALID_FILE_TYPE",
                "File size exceeds maximum allowed size of 50MB",
                "FILE_SIZE_EXCEEDED",
                "file");

        logger.info("Uploading document: {}, size: {}, extractMetadata: {}", originalFileName, file.getSize(),
                extractMetadata);

        FileMetadataSummary metadata = null;
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
        return response;
    }

    public FileUploadResponse uploadThumbnail(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        logger.info("Thumbnail upload started: {}, size: {}", originalFileName, file.getSize());

        fileValidationService.validateContentTypeAndSize(
                file,
                ALLOWED_IMAGE_TYPES,
                MAX_FILE_SIZE,
                "Invalid file type. Allowed types: " + ALLOWED_IMAGE_TYPES,
                "INVALID_FILE_TYPE",
                "File size exceeds maximum allowed size of 50MB",
                "FILE_SIZE_EXCEEDED",
                "file");
        fileValidationService.validateImageExtension(
                file,
                ALLOWED_IMAGE_EXTENSIONS,
                "Invalid image extension. Allowed extensions: " + ALLOWED_IMAGE_EXTENSIONS,
                "INVALID_IMAGE_EXTENSION",
                "file");
        fileValidationService.validateImageSignature(
                file,
                "Uploaded file is not a valid image",
                "INVALID_IMAGE_CONTENT",
                "Could not read uploaded image",
                "file");

        StoredFile storedFile = storageProvider.uploadFile(file, storageProperties.getThumbnailsFolder());

        FileUploadResponse response = new FileUploadResponse();
        response.setFileUrl(storedFile.fileUrl());
        response.setFileName(originalFileName);
        response.setFileSize(file.getSize());
        response.setFileType(file.getContentType());
        response.setObjectName(storedFile.objectName());

        logger.info("Thumbnail upload SUCCESS: {}", originalFileName);
        return response;
    }

    public FileDownloadResult downloadFile(String objectName) throws IOException {
        logger.info("Downloading file: {}", objectName);
        String normalizedObjectName = URLDecoder.decode(objectName, StandardCharsets.UTF_8).replaceFirst("^/", "");
        if (!storageProvider.fileExists(normalizedObjectName)) {
            logger.warn("File not found: {}", normalizedObjectName);
            throw new NotFoundException("File not found: " + normalizedObjectName, "FILE_NOT_FOUND", "objectName");
        }

        Resource resource = new InputStreamResource(storageProvider.downloadFileAsStream(normalizedObjectName));
        String filename = extractFilename(normalizedObjectName);
        logger.info("File download SUCCESS: {}", normalizedObjectName);
        return new FileDownloadResult(resource, filename);
    }

    public FileDeleteResponse deleteFile(String objectName) throws IOException {
        logger.info("Deleting file: {}", objectName);
        if (!storageProvider.fileExists(objectName)) {
            logger.warn("File not found for deletion: {}", objectName);
            throw new NotFoundException("File not found: " + objectName, "FILE_NOT_FOUND", "objectName");
        }

        storageProvider.deleteFile(objectName);
        logger.info("File deletion SUCCESS: {}", objectName);
        return new FileDeleteResponse(true, "File deleted successfully", objectName);
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
}
