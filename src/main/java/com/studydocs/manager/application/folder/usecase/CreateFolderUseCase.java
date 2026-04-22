package com.studydocs.manager.application.folder.usecase;

import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import com.studydocs.manager.service.folder.FolderEventService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CreateFolderUseCase {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final FileManagerAccessService accessService;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerResponseMapper fileManagerResponseMapper;
    private final FolderEventService folderEventService;

    public CreateFolderUseCase(
            FolderRepository folderRepository,
            UserRepository userRepository,
            FileManagerAccessService accessService,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerResponseMapper fileManagerResponseMapper,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
        this.folderEventService = folderEventService;
    }

    @Transactional
    public FolderResponse execute(FolderCreateRequest request) {
        Long userId = accessService.requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));

        String trimmedName = fileManagerNamePolicy.requireFolderName(request.getName());
        fileManagerNamespaceService.ensureFolderNameAvailable(userId, request.getParentId(), trimmedName, null);

        Folder folder = new Folder();
        folder.setUser(user);
        folder.setName(trimmedName);
        folder.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        if (request.getParentId() != null) {
            Folder parent = accessService.findActiveFolderForUser(request.getParentId(), userId);
            folder.setParent(parent);
        }

        Folder saved = folderRepository.save(folder);
        folderEventService.logCreated(saved);
        return fileManagerResponseMapper.toFolderResponse(saved);
    }
}
