package com.studydocs.manager.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinIOConfig.class);

    @Autowired
    private MinIOProperties minIOProperties;

    @Bean // Tạo Bean MinioClient
    public MinioClient minioClient(MinIOProperties properties) {
        logger.info("Initializing MinIO Client with endpoint: {}", properties.getEndpoint());

        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }


    @PostConstruct
    public void initBucket() {
        try {
            MinioClient client = minioClient(minIOProperties);

            String bucketName = minIOProperties.getBucketName();

            // Kiểm tra bucket có tồn tại không
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());

            // Nếu chưa có thì tạo bucket mới
            if (!exists) {
                client.makeBucket(
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
