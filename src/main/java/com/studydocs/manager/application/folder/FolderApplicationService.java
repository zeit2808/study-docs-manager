package com.studydocs.manager.application.folder;

import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderTrashResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.service.folder.FolderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FolderApplicationService {

    private final FolderService folderService;

    public FolderApplicationService(FolderService folderService) {
        this.folderService = folderService;
    }

    public FolderResponse createFolder(FolderCreateRequest request) {
        return folderService.createFolder(request);
    }

    public List<FolderResponse> getFolders(Long parentId) {
        return folderService.getMyFolders(parentId);
    }

    public FolderResponse getFolderById(Long id) {
        return folderService.getFolderById(id);
    }

    public List<FolderTrashResponse> getTrashFolders() {
        return folderService.getMyTrash();
    }

    public FolderResponse updateFolder(Long id, FolderUpdateRequest request) {
        return folderService.updateFolder(id, request);
    }

    public FolderDeleteResult deleteFolder(Long id) {
        return folderService.deleteFolder(id);
    }

    public void restoreFolder(Long id) {
        folderService.restoreFolder(id);
    }
}
