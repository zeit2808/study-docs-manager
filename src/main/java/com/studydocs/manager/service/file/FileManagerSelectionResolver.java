package com.studydocs.manager.service.file;

import com.studydocs.manager.dto.filemanager.FileManagerPasteItemRequest;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.enums.FileManagerItemType;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FileManagerSelectionResolver {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final FileManagerTreeService fileManagerTreeService;

    public FileManagerSelectionResolver(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            FileManagerTreeService fileManagerTreeService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.fileManagerTreeService = fileManagerTreeService;
    }

    public List<FileManagerSelection> canonicalizeOwnedSelection(List<FileManagerPasteItemRequest> items, Long userId) {
        Map<String, FileManagerPasteItemRequest> deduplicated = deduplicateItems(items);
        if (deduplicated.isEmpty()) {
            return List.of();
        }

        Map<Long, Folder> selectedFolders = new LinkedHashMap<>();
        Map<Long, Document> selectedDocuments = new LinkedHashMap<>();
        for (FileManagerPasteItemRequest item : deduplicated.values()) {
            if (item.getType() == FileManagerItemType.FOLDER) {
                Folder folder = folderRepository.findByIdAndDeletedAtIsNull(item.getId())
                        .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "items"));
                if (!folder.getUser().getId().equals(userId)) {
                    throw new ForbiddenException("Folder does not belong to current user", "FOLDER_ACCESS_DENIED", "items");
                }
                selectedFolders.put(folder.getId(), folder);
                continue;
            }

            Document document = documentRepository.findByIdAndDeletedAtIsNull(item.getId())
                    .orElseThrow(() -> new NotFoundException("Document not found", "DOCUMENT_NOT_FOUND", "items"));
            if (!document.getUser().getId().equals(userId)) {
                throw new ForbiddenException("Document does not belong to current user", "DOCUMENT_ACCESS_DENIED", "items");
            }
            selectedDocuments.put(document.getId(), document);
        }

        Set<Long> selectedFolderIds = selectedFolders.keySet();
        List<FileManagerSelection> canonical = new ArrayList<>();
        for (FileManagerPasteItemRequest item : deduplicated.values()) {
            if (item.getType() == FileManagerItemType.FOLDER) {
                Folder folder = selectedFolders.get(item.getId());
                if (folder != null && !fileManagerTreeService.hasSelectedAncestor(folder, selectedFolderIds)) {
                    canonical.add(FileManagerSelection.forFolder(folder));
                }
                continue;
            }

            Document document = selectedDocuments.get(item.getId());
            if (document != null && !fileManagerTreeService.isInsideSelectedFolder(document, selectedFolderIds)) {
                canonical.add(FileManagerSelection.forDocument(document));
            }
        }

        return canonical;
    }

    public LinkedHashSet<Long> normalizeIds(List<FileManagerPasteItemRequest> items, FileManagerItemType type) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (items == null || items.isEmpty()) {
            return ids;
        }

        for (FileManagerPasteItemRequest item : items) {
            if (item != null && item.getType() == type && item.getId() != null) {
                ids.add(item.getId());
            }
        }

        return ids;
    }

    private Map<String, FileManagerPasteItemRequest> deduplicateItems(List<FileManagerPasteItemRequest> items) {
        Map<String, FileManagerPasteItemRequest> deduplicated = new LinkedHashMap<>();
        if (items == null || items.isEmpty()) {
            return deduplicated;
        }

        for (FileManagerPasteItemRequest item : items) {
            if (item == null || item.getType() == null || item.getId() == null) {
                continue;
            }
            deduplicated.putIfAbsent(item.getType() + ":" + item.getId(), item);
        }

        return deduplicated;
    }
}
