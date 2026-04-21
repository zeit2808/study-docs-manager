package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FolderTrashCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(FolderTrashCleanupService.class);

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final boolean folderTrashCleanupEnabled;
    private final int folderTrashRetentionDays;

    public FolderTrashCleanupService(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            @Value("${cleanup.folder-trash.enabled:true}") boolean folderTrashCleanupEnabled,
            @Value("${cleanup.folder-trash.retention-days:90}") int folderTrashRetentionDays) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.folderTrashCleanupEnabled = folderTrashCleanupEnabled;
        this.folderTrashRetentionDays = folderTrashRetentionDays;
    }

    @Scheduled(cron = "${cleanup.folder-trash.cron:0 15 3 * * *}")
    @Transactional
    public void purgeExpiredFolderTrash() {
        if (!folderTrashCleanupEnabled) {
            logger.debug("Folder trash cleanup is disabled. Skipping...");
            return;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(folderTrashRetentionDays);
        List<Folder> expiredRoots = folderRepository.findDeletedRootFoldersBefore(cutoffDate);

        if (expiredRoots.isEmpty()) {
            logger.debug("No expired folder trees found for purge.");
            return;
        }

        int purgedTrees = 0;
        int purgedFolders = 0;
        int purgedDocuments = 0;

        for (Folder root : expiredRoots) {
            FolderTreeData treeData = loadTreeData(root);
            if (!treeData.canBePurged()) {
                logger.info("Skipping expired folder tree {} because file cleanup is not complete yet.", root.getId());
                continue;
            }

            List<Document> deletedDocuments = treeData.documents().stream()
                    .filter(document -> document.getDeletedAt() != null)
                    .toList();

            List<Folder> foldersDescending = treeData.folders().stream()
                    .sorted(Comparator.comparingInt(this::folderDepth).reversed())
                    .toList();

            documentRepository.deleteAll(deletedDocuments);
            folderRepository.deleteAll(foldersDescending);

            purgedTrees++;
            purgedFolders += foldersDescending.size();
            purgedDocuments += deletedDocuments.size();
        }

        logger.info("Folder trash purge completed: {} trees, {} folders, {} documents purged",
                purgedTrees, purgedFolders, purgedDocuments);
    }

    private FolderTreeData loadTreeData(Folder root) {
        List<Folder> folders = folderRepository.findByUserIdAndDeletedRootFolderId(root.getUser().getId(), root.getId());
        List<Long> folderIds = folders.stream()
                .map(Folder::getId)
                .toList();
        List<Document> documents = folderIds.isEmpty()
                ? List.of()
                : documentRepository.findAllByFolderIdIn(folderIds);

        Map<Long, String> objectNamesByDocumentId = documentIdsToObjectName(documents);
        return new FolderTreeData(folders, documents, objectNamesByDocumentId);
    }

    private Map<Long, String> documentIdsToObjectName(List<Document> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .toList();
        List<DocumentAsset> assets = documentAssetRepository.findByDocumentIdIn(documentIds);
        Map<Long, String> objectNamesByDocumentId = new HashMap<>();
        for (DocumentAsset asset : assets) {
            if (asset.getDocument() == null || asset.getDocument().getId() == null) {
                continue;
            }
            objectNamesByDocumentId.putIfAbsent(asset.getDocument().getId(), asset.getObjectName());
        }
        return objectNamesByDocumentId;
    }

    private int folderDepth(Folder folder) {
        int depth = 0;
        Folder current = folder.getParent();
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    private record FolderTreeData(
            List<Folder> folders,
            List<Document> documents,
            Map<Long, String> objectNamesByDocumentId) {

        private boolean canBePurged() {
            for (Document document : documents) {
                if (document.getDeletedAt() == null) {
                    return false;
                }

                String objectName = objectNamesByDocumentId.get(document.getId());
                if (objectName != null && !objectName.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }
}
