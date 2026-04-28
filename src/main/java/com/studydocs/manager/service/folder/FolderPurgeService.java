package com.studydocs.manager.service.folder;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.FolderEventRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.document.DocumentPurgeService;
import com.studydocs.manager.service.filemanager.FileManagerTreeService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Service
public class FolderPurgeService {

    private final FolderRepository folderRepository;
    private final FolderEventRepository folderEventRepository;
    private final FileManagerTreeService fileManagerTreeService;
    private final DocumentPurgeService documentPurgeService;

    public FolderPurgeService(
            FolderRepository folderRepository,
            FolderEventRepository folderEventRepository,
            FileManagerTreeService fileManagerTreeService,
            DocumentPurgeService documentPurgeService) {
        this.folderRepository = folderRepository;
        this.folderEventRepository = folderEventRepository;
        this.fileManagerTreeService = fileManagerTreeService;
        this.documentPurgeService = documentPurgeService;
    }

    @Transactional
    public int purgeDeletedTree(Folder root) throws IOException {
        List<Folder> folders = folderRepository.findByUserIdAndDeletedRootFolderId(root.getUser().getId(), root.getId());
        List<Document> documents = fileManagerTreeService.collectDeletedDocuments(folders);

        for (Document document : documents) {
            documentPurgeService.purge(document);
        }

        List<Folder> foldersDescending = folders.stream()
                .sorted(Comparator.comparingInt(fileManagerTreeService::folderDepth).reversed())
                .toList();
        List<Long> folderIds = foldersDescending.stream()
                .map(Folder::getId)
                .toList();

        folderEventRepository.deleteByFolderIdIn(folderIds);
        folderRepository.deleteAll(foldersDescending);
        return documents.size();
    }
}
