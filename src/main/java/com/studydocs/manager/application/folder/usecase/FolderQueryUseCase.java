package com.studydocs.manager.application.folder.usecase;

import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderTrashResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class FolderQueryUseCase {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final FileManagerAccessService accessService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    public FolderQueryUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            FileManagerAccessService accessService,
            FileManagerResponseMapper fileManagerResponseMapper) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.accessService = accessService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
    }

    public List<FolderResponse> getMyFolders(Long parentId) {
        Long userId = accessService.requireCurrentUserId();
        List<Folder> folders = (parentId == null)
                ? folderRepository.findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(userId)
                : folderRepository.findByUserIdAndParentIdAndDeletedAtIsNullOrderBySortOrder(userId, parentId);
        return folders.stream().map(fileManagerResponseMapper::toFolderResponse).collect(Collectors.toList());
    }

    public FolderResponse getFolderById(Long id) {
        Long userId = accessService.requireCurrentUserId();
        Folder folder = accessService.findActiveFolderForUser(id, userId);
        return fileManagerResponseMapper.toFolderResponse(folder);
    }

    public List<FolderTrashResponse> getMyTrash() {
        Long userId = accessService.requireCurrentUserId();
        Set<Long> restoreGroupRootIds = new HashSet<>(folderRepository.findDistinctDeletedRootFolderIdsByUserId(userId));
        restoreGroupRootIds.addAll(documentRepository.findDistinctDeletedRootFolderIdsByUserId(userId));

        List<Folder> restoreRoots = folderRepository.findAllById(restoreGroupRootIds).stream()
                .filter(folder -> folder.getUser().getId().equals(userId))
                .toList();

        return restoreRoots.stream()
                .map(this::toTrashResponse)
                .collect(Collectors.toList());
    }

    private FolderTrashResponse toTrashResponse(Folder root) {
        FolderTrashResponse response = new FolderTrashResponse();
        response.setId(root.getId());
        response.setName(root.getName());
        response.setParentId(root.getParent() != null ? root.getParent().getId() : null);
        response.setDeletedAt(root.getDeletedAt());

        List<Folder> foldersInTree = folderRepository.findByUserIdAndDeletedRootFolderId(root.getUser().getId(), root.getId());
        List<Document> documentsInTree = documentRepository.findByUserIdAndDeletedRootFolderId(root.getUser().getId(), root.getId());

        response.setDeletedDescendantFolderCount(Math.max(0, foldersInTree.size() - 1));
        response.setDeletedDocumentCount(documentsInTree.size());
        boolean restorable = hasRestorableDocuments(documentsInTree);
        response.setRestorable(restorable);
        if (!restorable) {
            response.setReason("FILE_CLEANED");
            response.setMessage("file is not exist");
        }
        return response;
    }

    private boolean hasRestorableDocuments(List<Document> documents) {
        if (documents.isEmpty()) {
            return true;
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
            if (fileManagerResponseMapper.wasFileCleaned(assetsByDocumentId.get(document.getId()))) {
                return false;
            }
        }
        return true;
    }
}
