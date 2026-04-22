package com.studydocs.manager.application.folder;

import com.studydocs.manager.application.filemanager.FileManagerApplicationService;
import com.studydocs.manager.application.folder.usecase.CreateFolderUseCase;
import com.studydocs.manager.application.folder.usecase.FolderQueryUseCase;
import com.studydocs.manager.application.folder.usecase.UpdateFolderUseCase;
import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderTrashResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderApplicationService {

    private final CreateFolderUseCase createFolderUseCase;
    private final UpdateFolderUseCase updateFolderUseCase;
    private final FolderQueryUseCase folderQueryUseCase;
    private final FileManagerApplicationService fileManagerApplicationService;

    public FolderApplicationService(
            CreateFolderUseCase createFolderUseCase,
            UpdateFolderUseCase updateFolderUseCase,
            FolderQueryUseCase folderQueryUseCase,
            FileManagerApplicationService fileManagerApplicationService) {
        this.createFolderUseCase = createFolderUseCase;
        this.updateFolderUseCase = updateFolderUseCase;
        this.folderQueryUseCase = folderQueryUseCase;
        this.fileManagerApplicationService = fileManagerApplicationService;
    }

    public FolderResponse createFolder(FolderCreateRequest request) {
        return createFolderUseCase.execute(request);
    }

    public List<FolderResponse> getFolders(Long parentId) {
        return folderQueryUseCase.getMyFolders(parentId);
    }

    public FolderResponse getFolderById(Long id) {
        return folderQueryUseCase.getFolderById(id);
    }

    public List<FolderTrashResponse> getTrashFolders() {
        return folderQueryUseCase.getMyTrash();
    }

    public FolderResponse updateFolder(Long id, FolderUpdateRequest request) {
        return updateFolderUseCase.execute(id, request);
    }

    public FolderDeleteResult deleteFolder(Long id) {
        return fileManagerApplicationService.deleteFolder(id);
    }

    public void restoreFolder(Long id) {
        fileManagerApplicationService.restoreFolder(id);
    }
}
