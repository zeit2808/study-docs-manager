package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
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
public class CreateDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateDocumentUseCase.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionService permissionService;
    private final DocumentAssetService assetService;
    private final DocumentTaxonomyService taxonomyService;
    private final DocumentActivityService activityService;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    public CreateDocumentUseCase(
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
    public DocumentResponse execute(DocumentCreateRequest request) {
        Long currentUserId = permissionService.requireCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));

        Folder folder = request.getFolderId() == null
                ? null
                : permissionService.validateFolderOwnership(request.getFolderId(), currentUserId);

        String resolvedDisplayName = fileManagerNamePolicy.requireDocumentName(
                request.getDisplayName(), request.getFileName(), request.getTitle());
        fileManagerNamespaceService.ensureDocumentNameAvailable(
                currentUserId,
                folder != null ? folder.getId() : null,
                resolvedDisplayName,
                null);

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
}
