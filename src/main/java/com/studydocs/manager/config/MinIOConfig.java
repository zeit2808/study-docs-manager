package com.studydocs.manager.config;

import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIOConfig - Configuration class cho MinIO
 * 
 * Chỉ chịu trách nhiệm định nghĩa beans, không làm initialization logic
 * Initialization được xử lý bởi MinIOBucketInitializer
 */
@Configuration
public class MinIOConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinIOConfig.class);

    /**
     * Tạo MinioClient bean
     * 
     * @param properties MinIO connection properties
     * @return Configured MinioClient instance
     */
    @Bean
    public MinioClient minioClient(MinIOProperties properties) {
        logger.info("Creating MinioClient bean with endpoint: {}", properties.getEndpoint());

        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
