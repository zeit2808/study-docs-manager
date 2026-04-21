package com.studydocs.manager.service.folder;

import com.studydocs.manager.application.file.FileManagerApplicationService;
import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderTrashResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.UnauthorizedException;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import com.studydocs.manager.service.file.FileManagerNamePolicy;
import com.studydocs.manager.service.file.FileManagerNamespaceService;
import com.studydocs.manager.service.file.FileManagerResponseMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FolderService {
    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerApplicationService fileManagerApplicationService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    public FolderService(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            UserRepository userRepository,
            SecurityUtils securityUtils,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerApplicationService fileManagerApplicationService,
            FileManagerResponseMapper fileManagerResponseMapper) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerApplicationService = fileManagerApplicationService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
    }

    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request) {
        Long userId = requireCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));

        validateUniqueName(userId, request.getName(), request.getParentId());

        Folder folder = new Folder();
        folder.setUser(user);
        folder.setName(request.getName().trim());
        folder.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        if (request.getParentId() != null) {
            Folder parent = findActiveFolderForUser(request.getParentId(), userId);
            folder.setParent(parent);
        }

        return fileManagerResponseMapper.toFolderResponse(folderRepository.save(folder));
    }

    public List<FolderResponse> getMyFolders(Long parentId) {
        Long userId = requireCurrentUser();
        List<Folder> folders = (parentId == null)
                ? folderRepository.findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(userId)
                : folderRepository.findByUserIdAndParentIdAndDeletedAtIsNullOrderBySortOrder(userId, parentId);
        return folders.stream().map(fileManagerResponseMapper::toFolderResponse).collect(Collectors.toList());
    }

    public FolderResponse getFolderById(Long id) {
        Long userId = requireCurrentUser();
        Folder folder = findActiveFolderForUser(id, userId);
        return fileManagerResponseMapper.toFolderResponse(folder);
    }

    public List<FolderTrashResponse> getMyTrash() {
        Long userId = requireCurrentUser();
        List<Folder> deletedRoots = folderRepository.findDeletedRootFoldersByUserId(userId);
        return deletedRoots.stream()
                .map(this::toTrashResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FolderResponse updateFolder(Long id, FolderUpdateRequest request) {
        Long userId = requireCurrentUser();
        Folder folder = findActiveFolderForUser(id, userId);
        Folder newParent = folder.getParent();

        if (request.isParentIdProvided()) {
            if (request.getParentId() != null) {
                if (request.getParentId().equals(id)) {
                    throw new BadRequestException("A folder cannot be its own parent", "INVALID_PARENT_FOLDER", "parentId");
                }
                newParent = findActiveFolderForUser(request.getParentId(), userId);
                validateNoCircularMove(folder, newParent);
            } else {
                newParent = null;
            }
        }

        String nextName = request.getName() != null ? request.getName().trim() : folder.getName();
        boolean nameChanged = !nextName.equals(folder.getName());
        boolean parentChanged = !sameFolder(folder.getParent(), newParent);
        if (nameChanged || parentChanged) {
            validateUniqueName(userId, nextName, newParent != null ? newParent.getId() : null, folder.getId());
        }

        if (request.getName() != null) {
            folder.setName(nextName);
        }

        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }

        if (request.isParentIdProvided()) {
            folder.setParent(newParent);
        }

        return fileManagerResponseMapper.toFolderResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderDeleteResult deleteFolder(Long id) {
        return fileManagerApplicationService.deleteFolder(id);
    }

    @Transactional
    public FolderResponse restoreFolder(Long id) {
        Folder restored = fileManagerApplicationService.restoreFolder(id);
        return fileManagerResponseMapper.toFolderResponse(restored);
    }

    private Folder findActiveFolderForUser(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(folderId)
                .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "id"));
        validateFolderOwnership(folder, userId);
        return folder;
    }

    private void validateFolderOwnership(Folder folder, Long userId) {
        if (!folder.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Folder does not belong to current user", "FOLDER_ACCESS_DENIED", "id");
        }
    }

    private Long requireCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated", "USER_NOT_AUTHENTICATED", null);
        }
        return userId;
    }

    private void validateUniqueName(Long userId, String name, Long parentId) {
        validateUniqueName(userId, name, parentId, null);
    }

    private void validateUniqueName(Long userId, String name, Long parentId, Long ignoredFolderId) {
        String trimmedName = fileManagerNamePolicy.requireFolderName(name);
        fileManagerNamespaceService.ensureFolderNameAvailable(userId, parentId, trimmedName, ignoredFolderId);
    }

    private void validateNoCircularMove(Folder folder, Folder newParent) {
        if (newParent == null) {
            return;
        }
        Folder current = newParent;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                throw new BadRequestException(
                        "A folder cannot be moved into itself or one of its descendants",
                        "INVALID_PARENT_FOLDER",
                        "parentId");
            }
            current = current.getParent();
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
        java.util.Map<Long, DocumentAsset> assetsByDocumentId = assets.stream()
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
