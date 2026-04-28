package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.folder.FolderEventService;
import org.springframework.stereotype.Service;

@Service
public class FolderRestorePathService {

    private final FolderRepository folderRepository;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FolderEventService folderEventService;

    public FolderRestorePathService(
            FolderRepository folderRepository,
            FileManagerNamespaceService fileManagerNamespaceService,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.folderEventService = folderEventService;
    }

    public Folder restoreDeletedAncestorChain(Folder folder, Long userId) {
        if (folder == null) {
            return null;
        }
        if (folder.getDeletedAt() == null) {
            return folder;
        }

        Folder parent = restoreDeletedAncestorChain(folder.getParent(), userId);
        Long parentId = parent != null ? parent.getId() : null;
        fileManagerNamespaceService.ensureAvailable(
                userId,
                parentId,
                folder.getName(),
                null,
                null,
                "Cannot restore folder because another active folder with the same name already exists in the target location.",
                "FOLDER_RESTORE_NAME_CONFLICT",
                "name");

        folder.restoreFromTrash();
        folder.setParent(parent);
        Folder saved = folderRepository.save(folder);
        folderEventService.logRestored(saved);
        return saved;
    }
}
