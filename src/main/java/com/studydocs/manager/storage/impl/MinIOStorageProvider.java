package com.studydocs.manager.storage.impl;

import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.storage.StorageProvider;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIOStorageProvider - MinIO implementation của StorageProvider
 * 
 * Bean này chỉ được load khi storage.provider=minio (hoặc không set, default là
 * minio)
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinIOStorageProvider implements StorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(MinIOStorageProvider.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOProperties minIOProperties;

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            // Tạo tên file unique: UUID + tên file gốc
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

            // Đường dẫn đầy đủ trong bucket
            String objectName = folder + fileName;

            logger.info("Uploading file to MinIO: bucket={}, object={}, size={}",
                    minIOProperties.getBucketName(), objectName, file.getSize());

            // Upload file lên MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            logger.info("File uploaded successfully to MinIO: {}", objectName);

            // Generate presigned URL 7 ngày
            String url = generatePresignedUrl(objectName, 7 * 24 * 60); // 7 days in minutes

            return url;

        } catch (Exception e) {
            logger.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new IOException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public void deleteFile(String objectNameOrUrl) throws IOException {
        try {
            String objectName = extractObjectName(objectNameOrUrl);

            logger.info("Deleting file from MinIO: {}", objectName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .build());

            logger.info("File deleted successfully from MinIO: {}", objectName);

        } catch (Exception e) {
            logger.error("Error deleting file from MinIO: {}", e.getMessage(), e);
            throw new IOException("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public InputStream downloadFileAsStream(String objectNameOrUrl) throws IOException {
        try {
            String objectName = extractObjectName(objectNameOrUrl);

            logger.info("Downloading file from MinIO: {}", objectName);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .build());

        } catch (Exception e) {
            logger.error("Error downloading file from MinIO: {}", e.getMessage(), e);
            throw new IOException("Failed to download file from MinIO", e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectNameOrUrl, int expirationMinutes) throws IOException {
        try {
            String objectName = extractObjectName(objectNameOrUrl);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build());

        } catch (Exception e) {
            logger.error("Error generating presigned URL: {}", e.getMessage(), e);
            throw new IOException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public boolean fileExists(String objectNameOrUrl) {
        try {
            String objectName = extractObjectName(objectNameOrUrl);

            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .build());
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract object name từ URL hoặc trả về as-is nếu đã là object name
     * 
     * Ví dụ:
     * - Input: "http://localhost:9000/studydocs/documents/abc.pdf"
     * - Output: "documents/abc.pdf"
     * 
     * - Input: "documents/abc.pdf"
     * - Output: "documents/abc.pdf"
     */
    private String extractObjectName(String objectNameOrUrl) {
        if (objectNameOrUrl == null) {
            throw new IllegalArgumentException("Object name or URL cannot be null");
        }

        // Nếu là URL đầy đủ (có http), extract object name
        if (objectNameOrUrl.startsWith("http://") || objectNameOrUrl.startsWith("https://")) {
            try {
                // URL format: http://localhost:9000/bucket-name/path/to/file.pdf
                // Cần lấy phần sau bucket name
                String[] parts = objectNameOrUrl.split("/" + minIOProperties.getBucketName() + "/");
                if (parts.length > 1) {
                    return parts[1];
                }
            } catch (Exception e) {
                logger.warn("Failed to extract object name from URL, using as-is: {}", objectNameOrUrl);
            }
        }

        // Nếu không phải URL hoặc extract thất bại, coi như đã là object name
        return objectNameOrUrl;
    }
}
