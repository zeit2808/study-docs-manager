package com.studydocs.manager.service;

import com.studydocs.manager.config.MinIOProperties;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIOService - Service để xử lý upload/download file lên MinIO
 * 
 * Giải thích:
 * - MinIO là object storage tương tự AWS S3
 * - Service này cung cấp các method để tương tác với MinIO
 * - Upload file, download file, delete file, generate URL
 */
@Service
public class MinIOService {

    private static final Logger logger = LoggerFactory.getLogger(MinIOService.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOProperties minIOProperties;

    /**
     * Upload file lên MinIO
     * 
     * @param file   - File từ client upload lên (MultipartFile)
     * @param folder - Thư mục trong bucket (vd: "documents/", "thumbnails/")
     * @return URL để truy cập file
     * 
     *         Flow hoạt động:
     *         1. Tạo tên file unique bằng UUID để tránh trùng lặp
     *         2. Upload file lên MinIO dưới dạng stream
     *         3. Generate presigned URL có thời hạn 7 ngày
     *         4. Trả về URL để client có thể download file
     */
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Tạo tên file unique: UUID + tên file gốc
        // Ví dụ: "a1b2c3d4-e5f6-7890-abcd-ef1234567890_document.pdf"
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // Đường dẫn đầy đủ trong bucket
        // Ví dụ: "documents/a1b2c3d4-e5f6-7890-abcd-ef1234567890_document.pdf"
        String objectName = folder + fileName;

        logger.info("Uploading file to MinIO: bucket={}, object={}, size={}",
                minIOProperties.getBucketName(), objectName, file.getSize());

        // Upload file lên MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minIOProperties.getBucketName()) // Tên bucket
                        .object(objectName) // Đường dẫn file trong bucket
                        .stream(file.getInputStream(), file.getSize(), -1) // Stream file, -1 = unknown part size
                        .contentType(file.getContentType()) // MIME type (application/pdf, image/png, ...)
                        .build());

        logger.info("File uploaded successfully: {}", objectName);

        // Generate URL có thời hạn để truy cập file
        // Presigned URL: URL tạm thời có chữ ký để truy cập file private
        String url = getFileUrl(objectName);

        return url;
    }

    /**
     * Generate presigned URL để truy cập file
     * 
     * @param objectName - Đường dẫn file trong bucket
     * @return URL có thời hạn 7 ngày
     * 
     *         Giải thích:
     *         - Presigned URL: URL có chữ ký xác thực, cho phép truy cập file
     *         private
     *         - Có thời hạn (expiry time), sau thời gian này URL sẽ không còn hiệu
     *         lực
     *         - Không cần authentication khi dùng URL này
     */
    public String getFileUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET) // HTTP Method: GET để download
                        .bucket(minIOProperties.getBucketName()) // Tên bucket
                        .object(objectName) // Đường dẫn file
                        .expiry(7, TimeUnit.DAYS) // URL hết hạn sau 7 ngày
                        .build());
    }

    /**
     * Download file từ MinIO
     * 
     * @param objectName - Đường dẫn file trong bucket
     * @return InputStream để đọc file
     * 
     *         Sử dụng:
     *         - Controller có thể stream file này trực tiếp về client
     *         - Hoặc lưu vào disk nếu cần
     */
    public InputStream downloadFile(String objectName) throws Exception {
        logger.info("Downloading file from MinIO: {}", objectName);

        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minIOProperties.getBucketName())
                        .object(objectName)
                        .build());
    }

    /**
     * Xóa file trên MinIO
     * 
     * @param objectName - Đường dẫn file trong bucket
     * 
     *                   Lưu ý:
     *                   - Method này XÓA VĨNH VIỄN file trên MinIO
     *                   - Nên cân nhắc soft delete trong database thay vì xóa file
     *                   thật
     */
    public void deleteFile(String objectName) throws Exception {
        logger.info("Deleting file from MinIO: {}", objectName);

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minIOProperties.getBucketName())
                        .object(objectName)
                        .build());

        logger.info("File deleted successfully: {}", objectName);
    }

    /**
     * Kiểm tra file có tồn tại không
     * 
     * @param objectName - Đường dẫn file trong bucket
     * @return true nếu file tồn tại, false nếu không
     */
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
}
