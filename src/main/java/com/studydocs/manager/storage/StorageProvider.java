package com.studydocs.manager.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * StorageProvider - Interface trừu tượng cho storage providers
 * 
 * Cho phép dễ dàng switch giữa các storage backends (MinIO, S3, GCS, Azure,
 * etc.)
 * mà không cần thay đổi business logic
 */
public interface StorageProvider {
    /**
     * Upload file lên storage
     * 
     * @param file   MultipartFile từ request
     * @param folder Folder/prefix để lưu file (vd: "documents/", "thumbnails/")
     * @return URL công khai để truy cập file
     */
    String uploadFile(MultipartFile file, String folder) throws IOException;

    /**
     * Xóa file từ storage
     * 
     * @param objectNameOrUrl Object name hoặc URL đầy đủ của file
     */
    void deleteFile(String objectNameOrUrl) throws IOException;

    /**
     * Download file dưới dạng InputStream (tối ưu cho file lớn)
     * 
     * @param objectNameOrUrl Object name hoặc URL đầy đủ của file
     * @return InputStream để đọc file content
     */
    InputStream downloadFileAsStream(String objectNameOrUrl) throws IOException;

    /**
     * Generate presigned URL có thời hạn để truy cập file
     * 
     * @param objectNameOrUrl   Object name hoặc URL đầy đủ của file
     * @param expirationMinutes Thời gian hết hạn (phút)
     * @return Presigned URL
     */
    String generatePresignedUrl(String objectNameOrUrl, int expirationMinutes) throws IOException;

    /**
     * Kiểm tra file có tồn tại không
     * 
     * @param objectNameOrUrl Object name hoặc URL đầy đủ của file
     * @return true nếu tồn tại, false nếu không
     */
    boolean fileExists(String objectNameOrUrl);
}
