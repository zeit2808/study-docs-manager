package com.studydocs.manager.application.folder.usecase;

import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import com.studydocs.manager.service.folder.FolderEventService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Handles metadata-only updates for a folder: rename and sort order.
 *
 * <p>Moving a folder to a different parent is a structural operation
 * and must be performed via {@code MoveItemsUseCase} (POST /filemanager/paste).
 */
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

        String oldName = folder.getName();
        String nextName = request.getName() != null ? request.getName().trim() : oldName;
        boolean nameChanged = !nextName.equals(oldName);

        if (nameChanged) {
            String validatedName = fileManagerNamePolicy.requireFolderName(nextName);
            fileManagerNamespaceService.ensureFolderNameAvailable(
                    userId,
                    folder.getParent() != null ? folder.getParent().getId() : null,
                    validatedName,
                    folder.getId());
            folder.setName(validatedName);
        }

        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }

        Folder saved = folderRepository.save(folder);

        if (nameChanged) {
            folderEventService.logRenamed(saved, oldName);
        }

        return fileManagerResponseMapper.toFolderResponse(saved);
    }
}
