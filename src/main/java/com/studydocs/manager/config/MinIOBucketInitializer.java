package com.studydocs.manager.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MinIOBucketInitializer - Component chịu trách nhiệm khởi tạo bucket
 * 
 * Tách riêng initialization logic ra khỏi configuration class
 * để tuân thủ Single Responsibility Principle
 */
@Component
public class MinIOBucketInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MinIOBucketInitializer.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOProperties minIOProperties;

    /**
     * Khởi tạo bucket khi application start
     * 
     * @PostConstruct được gọi sau khi bean đã được inject đầy đủ dependencies
     */
    @PostConstruct
    public void initBucket() {
        try {
            String bucketName = minIOProperties.getBucketName();

            logger.info("Initializing MinIO bucket: {}", bucketName);

            // Kiểm tra bucket có tồn tại không
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());

            // Nếu chưa có thì tạo bucket mới
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("Created MinIO bucket: {}", bucketName);
            } else {
                logger.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            logger.error("Error initializing MinIO bucket: {}", e.getMessage(), e);
        }
    }
}
