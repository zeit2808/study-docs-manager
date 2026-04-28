package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FileManagerTreeService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;

    public FileManagerTreeService(FolderRepository folderRepository, DocumentRepository documentRepository) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
    }

    public List<Folder> collectActiveFolderTree(Folder root) {
        List<Folder> folders = new ArrayList<>();
        folders.add(root);

        List<Folder> activeChildren = folderRepository.findByParentIdAndDeletedAtIsNullOrderBySortOrder(root.getId());
        for (Folder child : activeChildren) {
            folders.addAll(collectActiveFolderTree(child));
        }

        return folders;
    }

    public List<Folder> collectFolderSubtree(Folder root) {
        List<Folder> folders = new ArrayList<>();
        folders.add(root);

        List<Folder> children = folderRepository.findByParentIdOrderBySortOrder(root.getId());
        for (Folder child : children) {
            folders.addAll(collectFolderSubtree(child));
        }

        return folders;
    }

    public List<Folder> collectDeletedFolderTree(Folder root) {
        return collectFolderSubtree(root).stream()
                .filter(folder -> folder.getDeletedAt() != null)
                .toList();
    }

    public int countActiveDocuments(List<Folder> folderTree) {
        return collectActiveDocuments(folderTree).size();
    }

    public List<Document> collectActiveDocuments(List<Folder> folderTree) {
        List<Long> folderIds = folderTree.stream()
                .map(Folder::getId)
                .toList();
        if (folderIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllByFolderIdIn(folderIds).stream()
                .filter(document -> document.getDeletedAt() == null)
                .toList();
    }

    public List<Document> collectDeletedDocuments(List<Folder> folderTree) {
        List<Long> folderIds = folderTree.stream()
                .map(Folder::getId)
                .toList();
        if (folderIds.isEmpty()) {
            return List.of();
        }

        return documentRepository.findAllByFolderIdIn(folderIds).stream()
                .filter(document -> document.getDeletedAt() != null)
                .toList();
    }

    public boolean hasSelectedAncestor(Folder folder, Set<Long> selectedFolderIds) {
        Folder current = folder.getParent();
        while (current != null) {
            if (selectedFolderIds.contains(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public boolean isInsideSelectedFolder(Document document, Set<Long> selectedFolderIds) {
        Folder current = document.getFolder();
        while (current != null) {
            if (selectedFolderIds.contains(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public boolean isDescendantOrSelf(Folder candidateTarget, Folder sourceFolder) {
        Folder current = candidateTarget;
        while (current != null) {
            if (current.getId().equals(sourceFolder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public int folderDepth(Folder folder) {
        int depth = 0;
        Folder current = folder.getParent();
        while (current != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    public boolean sameFolder(Folder left, Folder right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.getId().equals(right.getId());
    }
}
