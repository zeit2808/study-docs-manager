package com.studydocs.manager.application.admin;

import com.studydocs.manager.dto.admin.AdminTrashPurgeRequest;
import com.studydocs.manager.dto.admin.AdminTrashPurgeResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.document.DocumentPurgeService;
import com.studydocs.manager.service.folder.FolderPurgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class AdminTrashCleanupApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(AdminTrashCleanupApplicationService.class);

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final DocumentPurgeService documentPurgeService;
    private final FolderPurgeService folderPurgeService;

    public AdminTrashCleanupApplicationService(
            DocumentRepository documentRepository,
            FolderRepository folderRepository,
            DocumentPurgeService documentPurgeService,
            FolderPurgeService folderPurgeService) {
        this.documentRepository = documentRepository;
        this.folderRepository = folderRepository;
        this.documentPurgeService = documentPurgeService;
        this.folderPurgeService = folderPurgeService;
    }

    public AdminTrashPurgeResponse purge(AdminTrashPurgeRequest request) {
        AdminTrashPurgeResponse response = new AdminTrashPurgeResponse();

        if (request.getScope() == AdminTrashPurgeRequest.Scope.DOCUMENTS || request.getScope() == AdminTrashPurgeRequest.Scope.ALL) {
            purgeDocuments(request.getUserId(), response);
        }

        if (request.getScope() == AdminTrashPurgeRequest.Scope.FOLDERS || request.getScope() == AdminTrashPurgeRequest.Scope.ALL) {
            purgeFolders(request.getUserId(), response);
        }

        return response;
    }

    private void purgeDocuments(Long userId, AdminTrashPurgeResponse response) {
        List<Document> documents = userId == null
                ? documentRepository.findByStatus(DocumentStatus.DELETED, org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                        .filter(document -> document.getDeletedRootFolderId() == null)
                        .toList()
                : documentRepository.findByUserIdAndStatus(userId, DocumentStatus.DELETED).stream()
                        .filter(document -> document.getDeletedRootFolderId() == null)
                        .toList();

        for (Document document : documents) {
            try {
                documentPurgeService.purge(document);
                response.setPurgedDocuments(response.getPurgedDocuments() + 1);
            } catch (IOException e) {
                response.getFailedDocumentIds().add(document.getId());
                logger.error("Admin trash purge failed for document id={}: {}", document.getId(), e.getMessage(), e);
            }
        }
    }

    private void purgeFolders(Long userId, AdminTrashPurgeResponse response) {
        List<Folder> roots = userId == null
                ? folderRepository.findDeletedRootFolders()
                : folderRepository.findDeletedRootFoldersByUserId(userId);

        for (Folder root : roots) {
            try {
                int purgedDocuments = folderPurgeService.purgeDeletedTree(root);
                response.setPurgedDocuments(response.getPurgedDocuments() + purgedDocuments);
                response.setPurgedFolderRoots(response.getPurgedFolderRoots() + 1);
            } catch (IOException e) {
                response.getFailedFolderRootIds().add(root.getId());
                logger.error("Admin trash purge failed for folder root id={}: {}", root.getId(), e.getMessage(), e);
            }
        }
    }
}
