package com.studydocs.manager.application.file;

import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.file.PresignedUploadRequest;
import com.studydocs.manager.dto.file.PresignedUploadResponse;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.service.file.FileValidationService;
import com.studydocs.manager.service.file.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadApplicationServiceTest {

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private TikaMetadataService tikaMetadataService;

    private StorageProperties storageProperties;
    private MinIOProperties minIOProperties;
    private FileValidationService fileValidationService;
    private FileUploadApplicationService fileUploadApplicationService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setDocumentsFolder("documents/");
        storageProperties.setThumbnailsFolder("thumbnails/");
        storageProperties.setAvatarsFolder("avatars/");

        minIOProperties = new MinIOProperties();
        minIOProperties.setEndpoint("http://localhost:9000");
        minIOProperties.setPresignedUploadExpiryMinutes(15);
        minIOProperties.setPresignedDownloadExpiryMinutes(30);

        fileValidationService = new FileValidationService();

        fileUploadApplicationService = new FileUploadApplicationService(
                storageProvider,
                storageProperties,
                minIOProperties,
                fileValidationService,
                tikaMetadataService);
    }

    @Test
    @DisplayName("Should successfully generate presigned upload URL for valid document")
    void testGeneratePresignedUploadUrl_Success() throws IOException {
        when(storageProvider.generatePresignedUploadUrl(anyString(), eq("application/pdf"), eq(15)))
                .thenReturn("http://localhost:9000/studydocs-documents/documents/uuid_test.pdf?signature=abc");

        PresignedUploadRequest request = new PresignedUploadRequest(
                "calculus_notes.pdf",
                1024L * 1024L,
                "application/pdf",
                "DOCUMENTS");

        PresignedUploadResponse response = fileUploadApplicationService.generatePresignedUploadUrl(request);

        assertNotNull(response);
        assertEquals("PUT", response.getHttpMethod());
        assertEquals(15, response.getExpiresInMinutes());
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getObjectName().startsWith("documents/"));
        assertTrue(response.getObjectName().endsWith("calculus_notes.pdf"));
        assertEquals("http://localhost:9000/studydocs-documents/documents/uuid_test.pdf?signature=abc", response.getUploadUrl());

        verify(storageProvider, times(1)).generatePresignedUploadUrl(anyString(), eq("application/pdf"), eq(15));
    }

    @Test
    @DisplayName("Should reject upload when file size exceeds 50MB limit")
    void testGeneratePresignedUploadUrl_SizeExceeded() {
        PresignedUploadRequest request = new PresignedUploadRequest(
                "huge_file.pdf",
                55L * 1024L * 1024L, // 55MB > 50MB
                "application/pdf",
                "DOCUMENTS");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> fileUploadApplicationService.generatePresignedUploadUrl(request));

        assertEquals("FILE_SIZE_EXCEEDED", ex.getCode());
    }

    @Test
    @DisplayName("Should reject upload when file extension is not allowed")
    void testGeneratePresignedUploadUrl_InvalidExtension() {
        PresignedUploadRequest request = new PresignedUploadRequest(
                "malicious_script.exe",
                1024L,
                "application/pdf",
                "DOCUMENTS");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> fileUploadApplicationService.generatePresignedUploadUrl(request));

        assertEquals("INVALID_FILE_EXTENSION", ex.getCode());
    }

    @Test
    @DisplayName("Should reject upload when content type is not allowed")
    void testGeneratePresignedUploadUrl_InvalidContentType() {
        PresignedUploadRequest request = new PresignedUploadRequest(
                "data.pdf",
                1024L,
                "application/x-executable",
                "DOCUMENTS");

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> fileUploadApplicationService.generatePresignedUploadUrl(request));

        assertEquals("INVALID_FILE_TYPE", ex.getCode());
    }
}
