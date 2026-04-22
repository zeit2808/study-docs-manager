package com.studydocs.manager.application.document.usecase;

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
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.document.DocumentActivityService;
import com.studydocs.manager.service.document.DocumentAssetService;
import com.studydocs.manager.service.document.DocumentPermissionService;
import com.studydocs.manager.service.document.DocumentTaxonomyService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpdateDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDocumentUseCase.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionService permissionService;
    private final DocumentAssetService assetService;
    private final DocumentTaxonomyService taxonomyService;
    private final DocumentActivityService activityService;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    public UpdateDocumentUseCase(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DocumentPermissionService permissionService,
            DocumentAssetService assetService,
            DocumentTaxonomyService taxonomyService,
            DocumentActivityService activityService,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerResponseMapper fileManagerResponseMapper) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.assetService = assetService;
        this.taxonomyService = taxonomyService;
        this.activityService = activityService;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
    }

    @Transactional
    public DocumentResponse execute(Long id, DocumentUpdateRequest request) {
        Long currentUserId = permissionService.requireCurrentUserId();
        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        permissionService.validateDocumentOwnership(document, currentUserId, "update");

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));
        DocumentAsset currentAsset = fileManagerResponseMapper.resolveAsset(document);
        String currentDisplayName = fileManagerNamePolicy.requireDocumentName(
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
        String resolvedDisplayName = fileManagerNamePolicy.requireDocumentName(requestedDisplayName, nextFileName, nextTitle);
        boolean folderChanged = !sameFolder(oldFolder, newFolder);
        boolean nameChanged = !resolvedDisplayName.equals(currentDisplayName);
        if (folderChanged || nameChanged) {
            fileManagerNamespaceService.ensureDocumentNameAvailable(
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

    private boolean sameFolder(Folder left, Folder right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }
}
