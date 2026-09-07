package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.config.MinIOProperties;
import com.studydocs.manager.dto.document.DocumentDownloadUrlResponse;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.document.DocumentPermissionService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import com.studydocs.manager.storage.StorageProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DocumentQueryUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionService permissionService;
    private final FileManagerResponseMapper fileManagerResponseMapper;
    private final StorageProvider storageProvider;
    private final MinIOProperties minIOProperties;

    public DocumentQueryUseCase(
            DocumentRepository documentRepository,
            DocumentPermissionService permissionService,
            FileManagerResponseMapper fileManagerResponseMapper,
            StorageProvider storageProvider,
            MinIOProperties minIOProperties) {
        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
        this.storageProvider = storageProvider;
        this.minIOProperties = minIOProperties;
    }

    public DocumentResponse getDocumentById(Long id) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        Long currentUserId = permissionService.getCurrentUserId();
        if (document.getVisibility() != DocumentVisibility.PUBLIC
                && (currentUserId == null || !document.getUser().getId().equals(currentUserId))) {
            throw new ForbiddenException("You don't have permission to access this document", "DOCUMENT_ACCESS_DENIED", "id");
        }

        return fileManagerResponseMapper.toDocumentResponse(document);
    }

    public DocumentDownloadUrlResponse getDocumentDownloadUrl(Long id) {
        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        Long currentUserId = permissionService.getCurrentUserId();
        if (document.getVisibility() != DocumentVisibility.PUBLIC
                && (currentUserId == null || !document.getUser().getId().equals(currentUserId))) {
            throw new ForbiddenException("You don't have permission to access this document", "DOCUMENT_ACCESS_DENIED", "id");
        }

        DocumentAsset asset = fileManagerResponseMapper.resolveAsset(document);
        if (asset == null || asset.getObjectName() == null || asset.getObjectName().isBlank()) {
            throw new NotFoundException("Document file not found", "DOCUMENT_FILE_NOT_FOUND", "objectName");
        }

        int expiryMinutes = minIOProperties.getPresignedDownloadExpiryMinutes();
        try {
            String downloadUrl = storageProvider.generatePresignedUrl(asset.getObjectName(), expiryMinutes);
            return new DocumentDownloadUrlResponse(
                    downloadUrl,
                    asset.getFileName(),
                    asset.getFileType(),
                    asset.getFileSize(),
                    expiryMinutes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    public Page<DocumentResponse> getMyDocuments(String status, Long folderId, Pageable pageable) {
        Long currentUserId = permissionService.requireCurrentUserId();
        Page<Document> documents;

        if (folderId != null) {
            documents = documentRepository.findByUserIdAndFolderIdAndDeletedAtIsNull(currentUserId, folderId, pageable);
        } else if (status != null) {
            try {
                DocumentStatus docStatus = DocumentStatus.valueOf(status);
                documents = documentRepository.findByUserIdAndStatusAndDeletedAtIsNull(currentUserId, docStatus, pageable);
            } catch (IllegalArgumentException e) {
                documents = documentRepository.findByUserIdAndDeletedAtIsNull(currentUserId, pageable);
            }
        } else {
            documents = documentRepository.findByUserIdAndDeletedAtIsNull(currentUserId, pageable);
        }

        return documents.map(fileManagerResponseMapper::toDocumentResponse);
    }

    public Page<DocumentResponse> getPublicDocuments(String status, Pageable pageable) {
        DocumentStatus docStatus = status != null ? DocumentStatus.valueOf(status) : DocumentStatus.PUBLISHED;
        Page<Document> documents = documentRepository.findByVisibilityAndStatusAndDeletedAtIsNull(
                DocumentVisibility.PUBLIC, docStatus, pageable);
        return documents.map(fileManagerResponseMapper::toDocumentResponse);
    }

    public Page<DocumentResponse> getMyTrash(Pageable pageable) {
        Long currentUserId = permissionService.requireCurrentUserId();
        return documentRepository
                .findByUserIdAndStatusAndDeletedAtIsNotNull(currentUserId, DocumentStatus.DELETED, pageable)
                .map(this::convertToTrashResponse);
    }

    private DocumentResponse convertToTrashResponse(Document document) {
        DocumentResponse response = fileManagerResponseMapper.toDocumentResponse(document);
        if (fileManagerResponseMapper.wasFileCleaned(fileManagerResponseMapper.resolveAsset(document))) {
            response.setReason("FILE_CLEANED");
            response.setMessage("file is not exist");
        }
        return response;
    }
}
