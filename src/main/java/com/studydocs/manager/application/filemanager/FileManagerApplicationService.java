package com.studydocs.manager.application.filemanager;

import com.studydocs.manager.dto.filemanager.FileManagerDeleteRequest;
import com.studydocs.manager.dto.filemanager.FileManagerDeleteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerTransferRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.application.filemanager.usecase.TransferItemsUseCase;
import com.studydocs.manager.application.filemanager.usecase.DeleteDocumentUseCase;
import com.studydocs.manager.application.filemanager.usecase.DeleteFolderUseCase;
import com.studydocs.manager.application.filemanager.usecase.DeleteItemsUseCase;
import com.studydocs.manager.application.filemanager.usecase.RestoreDocumentUseCase;
import com.studydocs.manager.application.filemanager.usecase.RestoreFolderUseCase;
import org.springframework.stereotype.Service;

@Service
public class FileManagerApplicationService {

    private final TransferItemsUseCase transferItemsUseCase;
    private final DeleteItemsUseCase deleteItemsUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final DeleteFolderUseCase deleteFolderUseCase;
    private final RestoreDocumentUseCase restoreDocumentUseCase;
    private final RestoreFolderUseCase restoreFolderUseCase;

    public FileManagerApplicationService(
            TransferItemsUseCase transferItemsUseCase,
            DeleteItemsUseCase deleteItemsUseCase,
            DeleteDocumentUseCase deleteDocumentUseCase,
            DeleteFolderUseCase deleteFolderUseCase,
            RestoreDocumentUseCase restoreDocumentUseCase,
            RestoreFolderUseCase restoreFolderUseCase) {
        this.transferItemsUseCase = transferItemsUseCase;
        this.deleteItemsUseCase = deleteItemsUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.deleteFolderUseCase = deleteFolderUseCase;
        this.restoreDocumentUseCase = restoreDocumentUseCase;
        this.restoreFolderUseCase = restoreFolderUseCase;
    }

    public FileManagerPasteResponse copy(FileManagerTransferRequest request) {
        return transferItemsUseCase.copy(request);
    }

    public FileManagerPasteResponse move(FileManagerTransferRequest request) {
        return transferItemsUseCase.move(request);
    }

    public FileManagerDeleteResponse delete(FileManagerDeleteRequest request) {
        return deleteItemsUseCase.execute(request);
    }

    public void deleteDocument(Long id) {
        deleteDocumentUseCase.execute(id);
    }

    public FolderDeleteResult deleteFolder(Long id) {
        return deleteFolderUseCase.execute(id);
    }

    public Document restoreDocument(Long id) {
        return restoreDocumentUseCase.execute(id);
    }

    public Folder restoreFolder(Long id) {
        return restoreFolderUseCase.execute(id);
    }
}
