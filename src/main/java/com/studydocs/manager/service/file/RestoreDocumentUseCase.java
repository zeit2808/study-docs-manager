package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import org.springframework.stereotype.Service;

@Service
public class RestoreDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileManagerAccessService fileManagerAccessService;
    private final FileManagerAssetStateService fileManagerAssetStateService;
    private final FileManagerNamingService fileManagerNamingService;
    private final FileManagerEventService fileManagerEventService;

    public RestoreDocumentUseCase(
            DocumentRepository documentRepository,
            FileManagerAccessService fileManagerAccessService,
            FileManagerAssetStateService fileManagerAssetStateService,
            FileManagerNamingService fileManagerNamingService,
            FileManagerEventService fileManagerEventService) {
        this.documentRepository = documentRepository;
        this.fileManagerAccessService = fileManagerAccessService;
        this.fileManagerAssetStateService = fileManagerAssetStateService;
        this.fileManagerNamingService = fileManagerNamingService;
        this.fileManagerEventService = fileManagerEventService;
    }

    public Document execute(Long id) {
        Long currentUserId = fileManagerAccessService.requireCurrentUserId();
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));

        if (document.getDeletedAt() == null) {
            throw new BadRequestException("Document is not deleted", "DOCUMENT_NOT_DELETED", "id");
        }
        if (!document.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You don't have permission to restore this document", "DOCUMENT_RESTORE_DENIED", "id");
        }
        if (document.getFolder() != null && document.getFolder().getDeletedAt() != null) {
            throw new BadRequestException(
                    "Folder is still in trash. Restore the folder first.",
                    "DOCUMENT_FOLDER_NOT_RESTORED",
                    "folderId");
        }

        DocumentAsset asset = fileManagerAssetStateService.resolveAsset(document);
        if (fileManagerAssetStateService.wasFileCleaned(asset)) {
            throw new BadRequestException(
                    "Document file was already cleaned up and can no longer be restored.",
                    "DOCUMENT_FILE_ALREADY_CLEANED",
                    "id");
        }

        String restoredDisplayName = fileManagerNamingService.resolveDocumentDisplayName(
                document.getDisplayName(),
                asset != null ? asset.getFileName() : null,
                document.getTitle());
        fileManagerNamingService.validateDocumentNameAvailable(
                currentUserId,
                document.getFolder() != null ? document.getFolder().getId() : null,
                restoredDisplayName,
                null);

        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setDeletedRootFolderId(null);
        document.setStatus(DocumentStatus.DRAFT);
        document.setDisplayName(restoredDisplayName);

        Document saved = documentRepository.save(document);
        fileManagerEventService.logDocumentEvent(saved, DocumentEventType.RESTORED, "Document restored");
        return saved;
    }
}
