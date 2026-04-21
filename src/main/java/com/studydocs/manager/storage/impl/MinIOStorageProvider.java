package com.studydocs.manager.storage.impl;

import com.studydocs.manager.storage.StoredFile;
import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.storage.StorageProvider;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final MinioClient minioClient;
    private final MinIOProperties minIOProperties;

    public MinIOStorageProvider(MinioClient minioClient, MinIOProperties minIOProperties) {
        this.minioClient = minioClient;
        this.minIOProperties = minIOProperties;
    }

    @Override
    public StoredFile uploadFile(MultipartFile file, String folder) throws IOException {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            // Tạo tên file unique: UUID + tên file gốc
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalFilename;

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
            String fileUrl = generatePresignedUrl(objectName, 7 * 24 * 60); // 7 days in minutes

            return new StoredFile(objectName, fileUrl);

        } catch (Exception e) {
            logger.error("Error uploading file to MinIO: {}", e.getMessage(), e);
            throw new IOException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public String copyFile(String sourceObjectName, String targetFolder, String originalFilename) throws IOException {
        try {
            String safeFilename = (originalFilename == null || originalFilename.isBlank())
                    ? extractFilename(sourceObjectName)
                    : originalFilename.trim();
            String targetObjectName = targetFolder + UUID.randomUUID() + "_" + safeFilename;

            logger.info("Copying object in MinIO: source={}, target={}", sourceObjectName, targetObjectName);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(targetObjectName)
                            .source(CopySource.builder()
                                    .bucket(minIOProperties.getBucketName())
                                    .object(sourceObjectName)
                                    .build())
                            .build());

            logger.info("Object copied successfully in MinIO: {}", targetObjectName);
            return targetObjectName;
        } catch (Exception e) {
            logger.error("Error copying file in MinIO: {}", e.getMessage(), e);
            throw new IOException("Failed to copy file in MinIO", e);
        }
    }

    @Override
    public void deleteFile(String objectName) throws IOException {
        try {
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
    public InputStream downloadFileAsStream(String objectName) throws IOException {
        try {
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
    public String generatePresignedUrl(String objectName, int expirationMinutes) throws IOException {
        try {
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
    public boolean fileExists(String objectName) {
        try {
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

    private String extractFilename(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }
}
