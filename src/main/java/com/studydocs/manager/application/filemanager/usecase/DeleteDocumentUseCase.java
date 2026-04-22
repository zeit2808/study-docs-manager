package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final FileManagerAccessService fileManagerAccessService;
    private final FileManagerEventService fileManagerEventService;

    public DeleteDocumentUseCase(
            DocumentRepository documentRepository,
            FileManagerAccessService fileManagerAccessService,
            FileManagerEventService fileManagerEventService) {
        this.documentRepository = documentRepository;
        this.fileManagerAccessService = fileManagerAccessService;
        this.fileManagerEventService = fileManagerEventService;
    }

    public void execute(Long id) {
        Long currentUserId = fileManagerAccessService.requireCurrentUserId();
        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "id"));
        fileManagerAccessService.validateDocumentOwnership(document, currentUserId, "delete");

        User actor = fileManagerAccessService.requireActor();
        softDeleteDocument(document, actor, null, true);
        fileManagerEventService.deleteFromIndex(document.getId());
    }

    void softDeleteDocument(Document document, User actor, Long deletedRootFolderId, boolean emitEvent) {
        document.markDeleted(actor, deletedRootFolderId, LocalDateTime.now());
        documentRepository.save(document);

        if (emitEvent) {
            fileManagerEventService.logDocumentEvent(document, DocumentEventType.DELETED, "Document deleted");
        }
    }
}
