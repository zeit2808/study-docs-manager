package com.studydocs.manager.application.file;

import com.studydocs.manager.dto.filemanager.FileManagerDeleteRequest;
import com.studydocs.manager.dto.filemanager.FileManagerDeleteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerTransferRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.service.file.FileManagerTransferWorkflow;
import com.studydocs.manager.service.file.DeleteDocumentUseCase;
import com.studydocs.manager.service.file.DeleteFolderUseCase;
import com.studydocs.manager.service.file.DeleteItemsUseCase;
import com.studydocs.manager.service.file.RestoreDocumentUseCase;
import com.studydocs.manager.service.file.RestoreFolderUseCase;
import org.springframework.stereotype.Service;

@Service
public class FileManagerApplicationService {

    private final FileManagerTransferWorkflow fileManagerTransferWorkflow;
    private final DeleteItemsUseCase deleteItemsUseCase;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final DeleteFolderUseCase deleteFolderUseCase;
    private final RestoreDocumentUseCase restoreDocumentUseCase;
    private final RestoreFolderUseCase restoreFolderUseCase;

    public FileManagerApplicationService(
            FileManagerTransferWorkflow fileManagerTransferWorkflow,
            DeleteItemsUseCase deleteItemsUseCase,
            DeleteDocumentUseCase deleteDocumentUseCase,
            DeleteFolderUseCase deleteFolderUseCase,
            RestoreDocumentUseCase restoreDocumentUseCase,
            RestoreFolderUseCase restoreFolderUseCase) {
        this.fileManagerTransferWorkflow = fileManagerTransferWorkflow;
        this.deleteItemsUseCase = deleteItemsUseCase;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.deleteFolderUseCase = deleteFolderUseCase;
        this.restoreDocumentUseCase = restoreDocumentUseCase;
        this.restoreFolderUseCase = restoreFolderUseCase;
    }

    public FileManagerPasteResponse copy(FileManagerTransferRequest request) {
        return fileManagerTransferWorkflow.copy(request);
    }

    public FileManagerPasteResponse move(FileManagerTransferRequest request) {
        return fileManagerTransferWorkflow.move(request);
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
