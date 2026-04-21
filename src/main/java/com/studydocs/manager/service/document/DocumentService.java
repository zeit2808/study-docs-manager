package com.studydocs.manager.service.document;

import com.studydocs.manager.application.filemanager.FileManagerApplicationService;
import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.dto.document.DocumentUpdateRequest;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.file.FileManagerNamePolicy;
import com.studydocs.manager.service.file.FileManagerNamespaceService;
import com.studydocs.manager.service.file.FileManagerResponseMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orchestration service for document use-cases.
 * Delegates cross-cutting concerns to specialised services:
 * <ul>
 *   <li>{@link DocumentPermissionService} – auth / ownership checks</li>
 *   <li>{@link DocumentAssetService} – file-asset persistence</li>
 *   <li>{@link DocumentTaxonomyService} – subject / tag management</li>
 *   <li>{@link DocumentActivityService} – event logging / search reindex</li>
 * </ul>
 */
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerApplicationService fileManagerApplicationService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    // Delegated services
    private final DocumentPermissionService permissionService;
    private final DocumentAssetService assetService;
    private final DocumentTaxonomyService taxonomyService;
    private final DocumentActivityService activityService;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerApplicationService fileManagerApplicationService,
            FileManagerResponseMapper fileManagerResponseMapper,
            DocumentPermissionService permissionService,
            DocumentAssetService assetService,
            DocumentTaxonomyService taxonomyService,
            DocumentActivityService activityService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerApplicationService = fileManagerApplicationService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
        this.permissionService = permissionService;
        this.assetService = assetService;
        this.taxonomyService = taxonomyService;
        this.activityService = activityService;
    }

    // ===== COMMAND USE-CASES =====

    @Transactional
    public DocumentResponse createDocument(DocumentCreateRequest request) {
        Long currentUserId = permissionService.requireCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));

        Folder folder = resolveAndValidateFolder(request.getFolderId(), currentUserId);

        String resolvedDisplayName = resolveDocumentDisplayName(
                request.getDisplayName(), request.getFileName(), request.getTitle());
        validateDocumentNameAvailable(currentUserId, folder != null ? folder.getId() : null, resolvedDisplayName, null);

        Document document = new Document();
        document.setUser(user);
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setDisplayName(resolvedDisplayName);
        document.setLanguage(request.getLanguage() != null ? request.getLanguage() : "vi");
        document.setStatus(DocumentStatus.DRAFT);
        document.setVisibility(DocumentVisibility.PRIVATE);
        document.setCreatedBy(user);
        if (folder != null) {
            document.setFolder(folder);
        }

        Document saved = documentRepository.save(document);

        assetService.upsertAsset(saved, request.getObjectName(), request.getFileName(),
                request.getFileSize(), request.getFileType(), request.getThumbnailObjectName());

        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            taxonomyService.assignSubjects(saved, request.getSubjectIds());
        }
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            taxonomyService.assignTags(saved, request.getTagNames());
        }

        activityService.logEvent(saved, DocumentEventType.CREATED, "Document created");
        activityService.scheduleReindex(saved.getId());

        logger.info("Document created - id: {}, title: {}, userId: {}", saved.getId(), saved.getTitle(), currentUserId);
        return fileManagerResponseMapper.toDocumentResponse(saved);
    }

    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request) {
        Long currentUserId = permissionService.requireCurrentUserId();
        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        permissionService.validateDocumentOwnership(document, currentUserId, "update");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));
        DocumentAsset currentAsset = fileManagerResponseMapper.resolveAsset(document);
        String currentDisplayName = resolveDocumentDisplayName(
                document.getDisplayName(),
                currentAsset != null ? currentAsset.getFileName() : null,
                document.getTitle());
        Folder oldFolder = document.getFolder();
        Folder newFolder = oldFolder;

        if (request.isFolderIdProvided()) {
            newFolder = request.getFolderId() != null
                    ? permissionService.validateFolderOwnership(request.getFolderId(), currentUserId)
                    : null;
        }

        String nextTitle = request.getTitle() != null ? request.getTitle() : document.getTitle();
        String nextFileName = request.getFileName() != null
                ? request.getFileName()
                : currentAsset != null ? currentAsset.getFileName() : null;
        String requestedDisplayName = request.getDisplayName() != null ? request.getDisplayName() : document.getDisplayName();
        String resolvedDisplayName = resolveDocumentDisplayName(requestedDisplayName, nextFileName, nextTitle);
        boolean folderChanged = !sameFolder(oldFolder, newFolder);
        boolean nameChanged = !resolvedDisplayName.equals(currentDisplayName);
        if (folderChanged || nameChanged) {
            validateDocumentNameAvailable(
                    currentUserId,
                    newFolder != null ? newFolder.getId() : null,
                    resolvedDisplayName,
                    document.getId());
        }

        applyFieldUpdates(document, request, resolvedDisplayName, newFolder);
        document.setUpdatedBy(currentUser);
        Document saved = documentRepository.save(document);

        if (assetService.hasAssetChanges(request)) {
            assetService.upsertAsset(saved, request.getObjectName(), request.getFileName(),
                    request.getFileSize(), request.getFileType(), request.getThumbnailObjectName());
        }
        if (request.getSubjectIds() != null) {
            taxonomyService.replaceSubjects(saved, request.getSubjectIds());
        }
        if (request.getTagNames() != null) {
            taxonomyService.replaceTags(saved, request.getTagNames());
        }

        activityService.logEvent(saved, DocumentEventType.UPDATED, "Document updated");
        activityService.scheduleReindex(saved.getId());

        logger.info("Document updated - id: {}, userId: {}", id, currentUserId);
        return fileManagerResponseMapper.toDocumentResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id) {
        fileManagerApplicationService.deleteDocument(id);
        logger.info("Document deleted (soft) via file-manager command flow - id: {}", id);
    }

    @Transactional
    public DocumentResponse restoreDocument(Long id) {
        Document restored = fileManagerApplicationService.restoreDocument(id);
        logger.info("Document restored via file-manager command flow - id: {}", id);
        return fileManagerResponseMapper.toDocumentResponse(restored);
    }

    @Transactional
    public void permanentDeleteDocument(Long id) {
        Long currentUserId = permissionService.requireCurrentUserId();
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        if (document.getDeletedAt() == null) {
            throw new BadRequestException(
                    "Document is not in trash. Use DELETE /api/documents/{id} to soft-delete first.",
                    "DOCUMENT_NOT_IN_TRASH",
                    "id");
        }
        if (!document.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(
                    "You don't have permission to permanently delete this document",
                    "DOCUMENT_PERMANENT_DELETE_DENIED",
                    "id");
        }

        documentRepository.delete(document);
        logger.info("Document permanently deleted - id: {}, userId: {}", id, currentUserId);
    }

    @Transactional
    public int emptyTrash() {
        Long currentUserId = permissionService.requireCurrentUserId();
        java.util.List<Document> trashItems = documentRepository.findByUserIdAndStatus(currentUserId, DocumentStatus.DELETED);
        if (trashItems.isEmpty()) {
            return 0;
        }

        documentRepository.deleteAll(trashItems);
        logger.info("Emptied trash for userId: {} ({} documents permanently deleted)",
                currentUserId, trashItems.size());
        return trashItems.size();
    }

    // ===== QUERY USE-CASES =====

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

    // ===== PRIVATE HELPERS =====

    private Folder resolveAndValidateFolder(Long folderId, Long currentUserId) {
        if (folderId == null) {
            return null;
        }
        return permissionService.validateFolderOwnership(folderId, currentUserId);
    }

    private void applyFieldUpdates(Document document, DocumentUpdateRequest request,
                                   String resolvedDisplayName, Folder newFolder) {
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }
        document.setDisplayName(resolvedDisplayName);
        if (request.getLanguage() != null) {
            document.setLanguage(request.getLanguage());
        }
        if (request.getStatus() != null) {
            try {
                document.setStatus(DocumentStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus(), "INVALID_DOCUMENT_STATUS", "status");
            }
        }
        if (request.getVisibility() != null) {
            try {
                document.setVisibility(DocumentVisibility.valueOf(request.getVisibility()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid visibility: " + request.getVisibility(),
                        "INVALID_DOCUMENT_VISIBILITY",
                        "visibility");
            }
        }
        if (request.getIsFeatured() != null) {
            document.setIsFeatured(request.getIsFeatured());
        }
        if (request.isFolderIdProvided()) {
            document.setFolder(newFolder);
        }
    }

    private void validateDocumentNameAvailable(Long userId, Long folderId, String candidateName, Long ignoredDocumentId) {
        fileManagerNamespaceService.ensureDocumentNameAvailable(userId, folderId, candidateName, ignoredDocumentId);
    }

    private String resolveDocumentDisplayName(String requestedDisplayName, String fileName, String title) {
        return fileManagerNamePolicy.requireDocumentName(requestedDisplayName, fileName, title);
    }

    private boolean sameFolder(Folder left, Folder right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getId().equals(right.getId());
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
