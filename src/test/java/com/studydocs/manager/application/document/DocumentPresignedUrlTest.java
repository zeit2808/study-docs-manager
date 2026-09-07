package com.studydocs.manager.application.document;

import com.studydocs.manager.application.document.usecase.CreateDocumentUseCase;
import com.studydocs.manager.application.document.usecase.DocumentQueryUseCase;
import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentDownloadUrlResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.document.DocumentActivityService;
import com.studydocs.manager.service.document.DocumentAssetService;
import com.studydocs.manager.service.document.DocumentPermissionService;
import com.studydocs.manager.service.document.DocumentTaxonomyService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import com.studydocs.manager.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentPresignedUrlTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentPermissionService permissionService;
    @Mock
    private DocumentAssetService assetService;
    @Mock
    private DocumentTaxonomyService taxonomyService;
    @Mock
    private DocumentActivityService activityService;
    @Mock
    private FileManagerNamePolicy fileManagerNamePolicy;
    @Mock
    private FileManagerNamespaceService fileManagerNamespaceService;
    @Mock
    private FileManagerResponseMapper fileManagerResponseMapper;
    @Mock
    private StorageProvider storageProvider;

    private MinIOProperties minIOProperties;
    private CreateDocumentUseCase createDocumentUseCase;
    private DocumentQueryUseCase documentQueryUseCase;

    @BeforeEach
    void setUp() {
        minIOProperties = new MinIOProperties();
        minIOProperties.setPresignedDownloadExpiryMinutes(30);

        createDocumentUseCase = new CreateDocumentUseCase(
                documentRepository,
                userRepository,
                permissionService,
                assetService,
                taxonomyService,
                activityService,
                fileManagerNamePolicy,
                fileManagerNamespaceService,
                fileManagerResponseMapper,
                storageProvider);

        documentQueryUseCase = new DocumentQueryUseCase(
                documentRepository,
                permissionService,
                fileManagerResponseMapper,
                storageProvider,
                minIOProperties);
    }

    @Test
    @DisplayName("CreateDocumentUseCase should reject creation when object does not exist on storage")
    void testCreateDocument_ObjectNotFoundOnStorage() {
        when(permissionService.requireCurrentUserId()).thenReturn(1L);
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileManagerNamePolicy.requireDocumentName(any(), any(), any())).thenReturn("test_doc.pdf");
        when(fileManagerNamePolicy.resolveCreateName(any(), any())).thenReturn("test_doc.pdf");

        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("Physics Chapter 1");
        request.setObjectName("documents/non_existent_file.pdf");
        request.setFileName("test_doc.pdf");
        request.setFileSize(2048L);

        when(storageProvider.fileExists("documents/non_existent_file.pdf")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> createDocumentUseCase.execute(request));

        assertEquals("FILE_NOT_FOUND_ON_STORAGE", ex.getCode());
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("DocumentQueryUseCase should generate presigned download URL for authorized user")
    void testGetDocumentDownloadUrl_Success() throws IOException {
        Document document = new Document();
        document.setId(10L);
        User owner = new User();
        owner.setId(1L);
        document.setUser(owner);
        document.setVisibility(DocumentVisibility.PRIVATE);

        DocumentAsset asset = new DocumentAsset();
        asset.setObjectName("documents/uuid_doc.pdf");
        asset.setFileName("original_doc.pdf");
        asset.setFileType("application/pdf");
        asset.setFileSize(5000L);
        document.setAsset(asset);

        when(documentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(document));
        when(permissionService.getCurrentUserId()).thenReturn(1L);
        when(fileManagerResponseMapper.resolveAsset(document)).thenReturn(asset);
        when(storageProvider.generatePresignedUrl("documents/uuid_doc.pdf", 30))
                .thenReturn("http://localhost:9000/bucket/documents/uuid_doc.pdf?sig=123");

        DocumentDownloadUrlResponse response = documentQueryUseCase.getDocumentDownloadUrl(10L);

        assertNotNull(response);
        assertEquals("http://localhost:9000/bucket/documents/uuid_doc.pdf?sig=123", response.getDownloadUrl());
        assertEquals("original_doc.pdf", response.getFileName());
        assertEquals("application/pdf", response.getFileType());
        assertEquals(5000L, response.getFileSize());
        assertEquals(30, response.getExpiresInMinutes());
    }
}
