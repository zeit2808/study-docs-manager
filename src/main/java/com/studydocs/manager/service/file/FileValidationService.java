package com.studydocs.manager.service.file;

import com.studydocs.manager.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Service
public class FileValidationService {

    public void validateContentTypeAndSize(
            MultipartFile file,
            List<String> allowedTypes,
            long maxFileSize,
            String invalidTypeMessage,
            String invalidTypeCode,
            String sizeExceededMessage,
            String sizeExceededCode,
            String field) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty", "FILE_EMPTY", field);
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException(invalidTypeMessage, invalidTypeCode, field);
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(sizeExceededMessage, sizeExceededCode, field);
        }
    }

    public void validateImageExtension(
            MultipartFile file,
            List<String> allowedExtensions,
            String invalidExtensionMessage,
            String invalidExtensionCode,
            String field) {
        String originalFileName = file.getOriginalFilename();
        String normalizedFileName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        boolean validExtension = allowedExtensions.stream().anyMatch(normalizedFileName::endsWith);
        if (!validExtension) {
            throw new BadRequestException(invalidExtensionMessage, invalidExtensionCode, field);
        }
    }

    public void validateImageSignature(
            MultipartFile file,
            String invalidContentMessage,
            String invalidContentCode,
            String readErrorMessage,
            String field) {
        try {
            byte[] bytes = file.getBytes();
            if (!isSupportedImage(bytes)) {
                throw new BadRequestException(invalidContentMessage, invalidContentCode, field);
            }
        } catch (IOException e) {
            throw new BadRequestException(readErrorMessage, invalidContentCode, field);
        }
    }

    private boolean isSupportedImage(byte[] bytes) {
        return isJpeg(bytes) || isPng(bytes) || isGif(bytes) || isWebp(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 0x47
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x38
                && (bytes[4] == 0x37 || bytes[4] == 0x39)
                && bytes[5] == 0x61;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 0x52
                && bytes[1] == 0x49
                && bytes[2] == 0x46
                && bytes[3] == 0x46
                && bytes[8] == 0x57
                && bytes[9] == 0x45
                && bytes[10] == 0x42
                && bytes[11] == 0x50;
    }
}
