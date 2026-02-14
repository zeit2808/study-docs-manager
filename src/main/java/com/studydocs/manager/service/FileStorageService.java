package com.studydocs.manager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads/avatars}")
    private String uploadDir;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp");

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public String storeAvatar(MultipartFile file, Long userId) throws IOException {

        validateFile(file);

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = userId + "_" + System.currentTimeMillis() + extension;

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/static/avatars/" + filename;
    }

    public void deleteAvatar(String avatarUrl) throws IOException {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }

        String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
        Path filePath = Paths.get(uploadDir).resolve(filename);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File size exceeds maximum allowed size of 5MB");
        }
    }
}
