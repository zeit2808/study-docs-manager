package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileManagerNamespaceService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final FileManagerNamePolicy fileManagerNamePolicy;

    public FileManagerNamespaceService(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            FileManagerNamePolicy fileManagerNamePolicy) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
    }

    public Set<String> loadNormalizedNames(Long userId, Long parentFolderId) {
        Set<String> names = new LinkedHashSet<>();

        List<Folder> folders = parentFolderId == null
                ? folderRepository.findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(userId)
                : folderRepository.findByUserIdAndParentIdAndDeletedAtIsNullOrderBySortOrder(userId, parentFolderId);
        for (Folder folder : folders) {
            String normalized = fileManagerNamePolicy.normalize(folder.getName());
            if (normalized != null) {
                names.add(normalized);
            }
        }

        List<Document> documents = parentFolderId == null
                ? documentRepository.findActiveRootByUserIdWithAsset(userId)
                : documentRepository.findActiveByUserIdAndFolderIdWithAsset(userId, parentFolderId);
        for (Document document : documents) {
            String normalized = fileManagerNamePolicy.normalize(fileManagerNamePolicy.effectiveDocumentName(document));
            if (normalized != null) {
                names.add(normalized);
            }
        }

        return names;
    }

    public boolean hasConflict(
            Long userId,
            Long parentFolderId,
            String candidateName,
            Long ignoredFolderId,
            Long ignoredDocumentId) {
        String normalizedCandidate = fileManagerNamePolicy.normalize(candidateName);
        if (normalizedCandidate == null) {
            return false;
        }

        List<Folder> folders = parentFolderId == null
                ? folderRepository.findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(userId)
                : folderRepository.findByUserIdAndParentIdAndDeletedAtIsNullOrderBySortOrder(userId, parentFolderId);
        for (Folder folder : folders) {
            if (ignoredFolderId != null && ignoredFolderId.equals(folder.getId())) {
                continue;
            }
            if (normalizedCandidate.equals(fileManagerNamePolicy.normalize(folder.getName()))) {
                return true;
            }
        }

        List<Document> documents = parentFolderId == null
                ? documentRepository.findActiveRootByUserIdWithAsset(userId)
                : documentRepository.findActiveByUserIdAndFolderIdWithAsset(userId, parentFolderId);
        for (Document document : documents) {
            if (ignoredDocumentId != null && ignoredDocumentId.equals(document.getId())) {
                continue;
            }
            if (normalizedCandidate.equals(fileManagerNamePolicy.normalize(fileManagerNamePolicy.effectiveDocumentName(document)))) {
                return true;
            }
        }

        return false;
    }

    public void ensureFolderNameAvailable(Long userId, Long parentFolderId, String candidateName, Long ignoredFolderId) {
        ensureAvailable(
                userId,
                parentFolderId,
                candidateName,
                ignoredFolderId,
                null,
                "An item with this name already exists in this location",
                "FOLDER_NAME_EXISTS",
                "name");
    }

    public void ensureDocumentNameAvailable(Long userId, Long parentFolderId, String candidateName, Long ignoredDocumentId) {
        ensureAvailable(
                userId,
                parentFolderId,
                candidateName,
                null,
                ignoredDocumentId,
                "An item with this name already exists in this location",
                "DOCUMENT_NAME_EXISTS",
                "displayName");
    }

    public void ensureAvailable(
            Long userId,
            Long parentFolderId,
            String candidateName,
            Long ignoredFolderId,
            Long ignoredDocumentId,
            String message,
            String code,
            String field) {
        if (hasConflict(userId, parentFolderId, candidateName, ignoredFolderId, ignoredDocumentId)) {
            throw new BadRequestException(message, code, field);
        }
    }
}
