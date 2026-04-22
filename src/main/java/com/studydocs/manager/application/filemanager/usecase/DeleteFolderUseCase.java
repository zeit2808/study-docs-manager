package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerEventService;
import com.studydocs.manager.service.filemanager.FileManagerTreeService;
import com.studydocs.manager.service.folder.FolderEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeleteFolderUseCase {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final FileManagerTreeService fileManagerTreeService;
    private final FileManagerAccessService fileManagerAccessService;
    private final FileManagerEventService fileManagerEventService;
    private final FolderEventService folderEventService;

    public DeleteFolderUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            FileManagerTreeService fileManagerTreeService,
            FileManagerAccessService fileManagerAccessService,
            FileManagerEventService fileManagerEventService,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.fileManagerTreeService = fileManagerTreeService;
        this.fileManagerAccessService = fileManagerAccessService;
        this.fileManagerEventService = fileManagerEventService;
        this.folderEventService = folderEventService;
    }

    public FolderDeleteResult execute(Long id) {
        Long currentUserId = fileManagerAccessService.requireCurrentUserId();
        Folder folder = fileManagerAccessService.findActiveFolderForUser(id, currentUserId);
        User actor = fileManagerAccessService.requireActor();

        List<Document> deletedDocuments = softDeleteFolderTree(folder, actor);
        deletedDocuments.stream().map(Document::getId).forEach(fileManagerEventService::deleteFromIndex);
        folderEventService.logDeleted(folder);
        return new FolderDeleteResult(id, deletedDocuments.size());
    }

    private List<Document> softDeleteFolderTree(Folder root, User actor) {
        Long rootFolderId = root.getId();
        LocalDateTime now = LocalDateTime.now();

        List<Folder> foldersToDelete = fileManagerTreeService.collectActiveFolderTree(root);
        List<Document> documentsToDelete = fileManagerTreeService.collectActiveDocuments(foldersToDelete);

        for (Folder folder : foldersToDelete) {
            folder.markDeleted(actor, rootFolderId, now);
        }

        for (Document document : documentsToDelete) {
            document.markDeleted(actor, rootFolderId, now);
        }

        folderRepository.saveAll(foldersToDelete);
        documentRepository.saveAll(documentsToDelete);
        return documentsToDelete;
    }
}
