package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.document.DocumentPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmptyTrashDocumentsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EmptyTrashDocumentsUseCase.class);

    private final DocumentRepository documentRepository;
    private final DocumentPermissionService permissionService;

    public EmptyTrashDocumentsUseCase(
            DocumentRepository documentRepository,
            DocumentPermissionService permissionService) {
        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
    }

    public int execute() {
        Long currentUserId = permissionService.requireCurrentUserId();
        List<Document> trashItems = documentRepository.findByUserIdAndStatus(currentUserId, DocumentStatus.DELETED);
        if (trashItems.isEmpty()) {
            return 0;
        }

        documentRepository.deleteAll(trashItems);
        logger.info("Emptied trash for userId: {} ({} documents permanently deleted)",
                currentUserId, trashItems.size());
        return trashItems.size();
    }
}
