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
    StoredFile uploadFile(MultipartFile file, String folder) throws IOException;

    /**
     * Copy file already stored in the provider into a new object.
     *
     * @param sourceObjectName Source object name or full URL
     * @param targetFolder          Target folder/prefix (e.g. "documents/")
     * @param originalFilename      Original filename to preserve in the copied object name
     * @return Newly created object name
     */
    String copyFile(String sourceObjectName, String targetFolder, String originalFilename) throws IOException;

    /**
     * Xóa file từ storage
     * 
     * @param objectName Object name
     */
    void deleteFile(String objectName) throws IOException;

    /**
     * Download file dưới dạng InputStream (tối ưu cho file lớn)
     * 
     * @param objectName Object name
     * @return InputStream để đọc file content
     */
    InputStream downloadFileAsStream(String objectName) throws IOException;

    /**
     * Generate presigned URL có thời hạn để truy cập file
     * 
     * @param objectName   Object name
     * @param expirationMinutes Thời gian hết hạn (phút)
     * @return Presigned URL
     */
    String generatePresignedUrl(String objectName, int expirationMinutes) throws IOException;

    /**
     * Kiểm tra file có tồn tại không
     * 
     * @param objectName Object name
     * @return true nếu tồn tại, false nếu không
     */
    boolean fileExists(String objectName);
}
