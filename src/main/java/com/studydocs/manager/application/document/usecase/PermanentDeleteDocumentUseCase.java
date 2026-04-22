package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.document.DocumentPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PermanentDeleteDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PermanentDeleteDocumentUseCase.class);

    private final DocumentRepository documentRepository;
    private final DocumentPermissionService permissionService;

    public PermanentDeleteDocumentUseCase(
            DocumentRepository documentRepository,
            DocumentPermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
    }

    public void execute(Long id) {
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
}
