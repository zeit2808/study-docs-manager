package com.studydocs.manager.application.folder.usecase;

import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import com.studydocs.manager.service.folder.FolderEventService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UpdateFolderUseCase {

    private final FolderRepository folderRepository;
    private final FileManagerAccessService accessService;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerResponseMapper fileManagerResponseMapper;
    private final FolderEventService folderEventService;

    public UpdateFolderUseCase(
            FolderRepository folderRepository,
            FileManagerAccessService accessService,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerResponseMapper fileManagerResponseMapper,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.accessService = accessService;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
        this.folderEventService = folderEventService;
    }

    @Transactional
    public FolderResponse execute(Long id, FolderUpdateRequest request) {
        Long userId = accessService.requireCurrentUserId();
        Folder folder = accessService.findActiveFolderForUser(id, userId);

        // Capture snapshots trước khi mutate
        String oldName = folder.getName();
        Folder oldParent = folder.getParent();

        Folder newParent = oldParent;
        if (request.isParentIdProvided()) {
            if (request.getParentId() != null) {
                if (request.getParentId().equals(id)) {
                    throw new BadRequestException("A folder cannot be its own parent", "INVALID_PARENT_FOLDER", "parentId");
                }
                newParent = accessService.findActiveFolderForUser(request.getParentId(), userId);
                validateNoCircularMove(folder, newParent);
            } else {
                newParent = null;
            }
        }

        String nextName = request.getName() != null ? request.getName().trim() : folder.getName();
        boolean nameChanged = !nextName.equals(oldName);
        boolean parentChanged = !sameFolder(oldParent, newParent);

        if (nameChanged || parentChanged) {
            String trimmedName = fileManagerNamePolicy.requireFolderName(nextName);
            fileManagerNamespaceService.ensureFolderNameAvailable(
                    userId, newParent != null ? newParent.getId() : null, trimmedName, folder.getId());
            nextName = trimmedName;
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

        Folder saved = folderRepository.save(folder);

        // Log events sau khi lưu thành công
        if (nameChanged && !parentChanged) {
            folderEventService.logRenamed(saved, oldName);
        } else if (parentChanged && !nameChanged) {
            folderEventService.logMoved(saved, oldParent);
        } else if (nameChanged) {
            // Cả hai thay đổi cùng lúc: log RENAMED trước, MOVED sau
            folderEventService.logRenamed(saved, oldName);
            folderEventService.logMoved(saved, oldParent);
        }

        return fileManagerResponseMapper.toFolderResponse(saved);
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
}
