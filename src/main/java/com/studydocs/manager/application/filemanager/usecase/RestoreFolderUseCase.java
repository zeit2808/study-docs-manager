package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerAssetStateService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerNamingService;
import com.studydocs.manager.service.filemanager.FileManagerTreeService;
import com.studydocs.manager.service.folder.FolderEventService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RestoreFolderUseCase {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final FileManagerTreeService fileManagerTreeService;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerAccessService fileManagerAccessService;
    private final FileManagerAssetStateService fileManagerAssetStateService;
    private final FileManagerNamingService fileManagerNamingService;
    private final FolderEventService folderEventService;

    public RestoreFolderUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            FileManagerTreeService fileManagerTreeService,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerAccessService fileManagerAccessService,
            FileManagerAssetStateService fileManagerAssetStateService,
            FileManagerNamingService fileManagerNamingService,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.fileManagerTreeService = fileManagerTreeService;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerAccessService = fileManagerAccessService;
        this.fileManagerAssetStateService = fileManagerAssetStateService;
        this.fileManagerNamingService = fileManagerNamingService;
        this.folderEventService = folderEventService;
    }

    public Folder execute(Long id) {
        Long userId = fileManagerAccessService.requireCurrentUserId();
        Folder root = fileManagerAccessService.findFolderForUser(id, userId);

        if (root.getDeletedAt() == null) {
            throw new BadRequestException("Folder is not deleted", "FOLDER_NOT_DELETED", "id");
        }

        Folder parent = root.getParent();
        if (parent != null && parent.getDeletedAt() != null) {
            throw new BadRequestException(
                    "Parent folder is still in trash. Restore the parent folder first.",
                    "PARENT_FOLDER_NOT_RESTORED",
                    "id");
        }

        List<Folder> foldersToRestore = new ArrayList<>(folderRepository.findByUserIdAndDeletedRootFolderId(userId, root.getId()));
        if (foldersToRestore.isEmpty()) {
            foldersToRestore.add(root);
        }

        Set<Long> restoringFolderIds = foldersToRestore.stream()
                .map(Folder::getId)
                .collect(Collectors.toCollection(HashSet::new));

        validateRestoreFolderConflicts(foldersToRestore, userId, restoringFolderIds);

        List<Document> documentsToRestore = documentRepository.findByUserIdAndDeletedRootFolderId(userId, root.getId());
        validateRestoredDocumentFolders(documentsToRestore, restoringFolderIds);
        validateRestoreDocumentConflicts(documentsToRestore, userId);
        ensureRestorableDocuments(
                documentsToRestore,
                "Folder tree cannot be restored because one or more files were already cleaned up.");

        foldersToRestore.sort(Comparator.comparingInt(fileManagerTreeService::folderDepth));
        for (Folder folder : foldersToRestore) {
            folder.restoreFromTrash();
        }

        for (Document document : documentsToRestore) {
            DocumentAsset asset = fileManagerAssetStateService.resolveAsset(document);
            String restoredDisplayName = fileManagerNamingService.resolveDocumentDisplayName(
                    document.getDisplayName(),
                    asset != null ? asset.getFileName() : null,
                    document.getTitle());
            document.restoreFromTrash(restoredDisplayName);
        }

        folderRepository.saveAll(foldersToRestore);
        documentRepository.saveAll(documentsToRestore);
        folderEventService.logRestored(root);
        return root;
    }

    private void validateRestoreFolderConflicts(List<Folder> foldersToRestore, Long userId, Set<Long> restoringFolderIds) {
        for (Folder folder : foldersToRestore) {
            Folder parent = folder.getParent();
            if (parent != null && parent.getDeletedAt() != null && !restoringFolderIds.contains(parent.getId())) {
                throw new BadRequestException(
                        "Parent folder is still in trash. Restore the parent folder first.",
                        "PARENT_FOLDER_NOT_RESTORED",
                        "id");
            }

            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
            fileManagerNamespaceService.ensureAvailable(
                    userId,
                    parentId,
                    folder.getName(),
                    null,
                    null,
                    "Cannot restore folder because another active folder with the same name already exists in the target location.",
                    "FOLDER_RESTORE_NAME_CONFLICT",
                    "name");
        }
    }

    private void validateRestoredDocumentFolders(List<Document> documentsToRestore, Set<Long> restoringFolderIds) {
        for (Document document : documentsToRestore) {
            Folder folder = document.getFolder();
            if (folder != null && folder.getDeletedAt() != null && !restoringFolderIds.contains(folder.getId())) {
                throw new BadRequestException(
                        "A document in this folder tree still points to a folder in trash. Restore the folder tree in order.",
                        "DOCUMENT_FOLDER_NOT_RESTORED",
                        "id");
            }
        }
    }

    private void validateRestoreDocumentConflicts(List<Document> documentsToRestore, Long userId) {
        for (Document document : documentsToRestore) {
            Long parentId = document.getFolder() != null ? document.getFolder().getId() : null;
            String effectiveName = fileManagerNamePolicy.effectiveDocumentName(document);
            fileManagerNamespaceService.ensureAvailable(
                    userId,
                    parentId,
                    effectiveName,
                    null,
                    null,
                    "Cannot restore document because another active item with the same name already exists in the target location.",
                    "DOCUMENT_RESTORE_NAME_CONFLICT",
                    "displayName");
        }
    }

    private void ensureRestorableDocuments(List<Document> documents, String message) {
        if (documents.isEmpty()) {
            return;
        }

        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .toList();
        List<DocumentAsset> assets = documentAssetRepository.findByDocumentIdIn(documentIds);
        Map<Long, DocumentAsset> assetsByDocumentId = assets.stream()
                .collect(Collectors.toMap(
                        asset -> asset.getDocument().getId(),
                        asset -> asset,
                        (left, right) -> left));

        for (Document document : documents) {
            if (fileManagerAssetStateService.wasFileCleaned(assetsByDocumentId.get(document.getId()))) {
                throw new BadRequestException(message, "FOLDER_TREE_FILE_ALREADY_CLEANED", "id");
            }
        }
    }
}
