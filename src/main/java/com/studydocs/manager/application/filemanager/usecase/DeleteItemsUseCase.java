package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.dto.filemanager.FileManagerDeleteRequest;
import com.studydocs.manager.dto.filemanager.FileManagerDeleteResponse;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.FileManagerItemType;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.file.FileManagerAccessService;
import com.studydocs.manager.service.file.FileManagerSelectionResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeleteItemsUseCase {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final FileManagerSelectionResolver fileManagerSelectionResolver;
    private final FileManagerAccessService fileManagerAccessService;
    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final DeleteFolderUseCase deleteFolderUseCase;

    public DeleteItemsUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            FileManagerSelectionResolver fileManagerSelectionResolver,
            FileManagerAccessService fileManagerAccessService,
            DeleteDocumentUseCase deleteDocumentUseCase,
            DeleteFolderUseCase deleteFolderUseCase) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.fileManagerSelectionResolver = fileManagerSelectionResolver;
        this.fileManagerAccessService = fileManagerAccessService;
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.deleteFolderUseCase = deleteFolderUseCase;
    }

    public FileManagerDeleteResponse execute(FileManagerDeleteRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required", "BATCH_DELETE_REQUEST_REQUIRED", null);
        }

        Set<Long> folderIds = fileManagerSelectionResolver.normalizeIds(request.getItems(), FileManagerItemType.FOLDER);
        Set<Long> documentIds = fileManagerSelectionResolver.normalizeIds(request.getItems(), FileManagerItemType.DOCUMENT);
        if (folderIds.isEmpty() && documentIds.isEmpty()) {
            throw new BadRequestException(
                    "At least one folder or document must be selected",
                    "BATCH_DELETE_EMPTY_SELECTION",
                    null);
        }

        User actor = fileManagerAccessService.requireActor();
        fileManagerAccessService.validateCurrentFolderContext(request.getCurrentFolderId(), actor);
        List<Folder> folders = validateDeleteFolders(folderIds, actor.getId(), request.getCurrentFolderId());
        List<Document> documents = validateDeleteDocuments(documentIds, actor, request.getCurrentFolderId());

        int cascadeDeletedDocumentCount = 0;
        List<Long> deletedFolderIds = new ArrayList<>();
        for (Folder folder : folders) {
            FolderDeleteResult result = deleteFolderUseCase.execute(folder.getId());
            cascadeDeletedDocumentCount += result.getAffectedDocuments();
            deletedFolderIds.add(folder.getId());
        }

        List<Long> deletedDocumentIds = new ArrayList<>();
        for (Document document : documents) {
            deleteDocumentUseCase.execute(document.getId());
            deletedDocumentIds.add(document.getId());
        }

        return new FileManagerDeleteResponse(
                request.getCurrentFolderId(),
                deletedFolderIds,
                deletedDocumentIds,
                cascadeDeletedDocumentCount);
    }

    private List<Folder> validateDeleteFolders(Set<Long> folderIds, Long actorId, Long currentFolderId) {
        if (folderIds.isEmpty()) {
            return List.of();
        }

        List<Folder> folders = folderRepository.findByIdInAndDeletedAtIsNull(new ArrayList<>(folderIds));
        Map<Long, Folder> foldersById = folders.stream()
                .collect(Collectors.toMap(Folder::getId, folder -> folder));

        List<Long> missingIds = folderIds.stream()
                .filter(id -> !foldersById.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new NotFoundException(
                    "Selected folder(s) not found or already deleted: " + missingIds,
                    "BATCH_DELETE_FOLDER_NOT_FOUND",
                    "items");
        }

        List<Folder> orderedFolders = new ArrayList<>();
        for (Long folderId : folderIds) {
            Folder folder = foldersById.get(folderId);
            if (!folder.getUser().getId().equals(actorId)) {
                throw new ForbiddenException(
                        "You don't have permission to delete one or more selected folders",
                        "BATCH_DELETE_FOLDER_DENIED",
                        "items");
            }

            Long actualParentId = folder.getParent() != null ? folder.getParent().getId() : null;
            if (!java.util.Objects.equals(actualParentId, currentFolderId)) {
                throw new BadRequestException(
                        "Selected folders must belong to the current level",
                        "BATCH_DELETE_FOLDER_LEVEL_MISMATCH",
                        "items");
            }
            orderedFolders.add(folder);
        }

        return orderedFolders;
    }

    private List<Document> validateDeleteDocuments(Set<Long> documentIds, User actor, Long currentFolderId) {
        if (documentIds.isEmpty()) {
            return List.of();
        }

        List<Document> documents = documentRepository.findByIdInAndDeletedAtIsNull(new ArrayList<>(documentIds));
        Map<Long, Document> documentsById = documents.stream()
                .collect(Collectors.toMap(Document::getId, document -> document));

        List<Long> missingIds = documentIds.stream()
                .filter(id -> !documentsById.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new NotFoundException(
                    "Selected document(s) not found or already deleted: " + missingIds,
                    "BATCH_DELETE_DOCUMENT_NOT_FOUND",
                    "items");
        }

        List<Document> orderedDocuments = new ArrayList<>();
        for (Long documentId : documentIds) {
            Document document = documentsById.get(documentId);
            if (!document.getUser().getId().equals(actor.getId()) && !fileManagerAccessService.isAdmin(actor)) {
                throw new ForbiddenException(
                        "You don't have permission to delete one or more selected documents",
                        "BATCH_DELETE_DOCUMENT_DENIED",
                        "items");
            }

            Long actualFolderId = document.getFolder() != null ? document.getFolder().getId() : null;
            if (!java.util.Objects.equals(actualFolderId, currentFolderId)) {
                throw new BadRequestException(
                        "Selected documents must belong to the current level",
                        "BATCH_DELETE_DOCUMENT_LEVEL_MISMATCH",
                        "items");
            }
            orderedDocuments.add(document);
        }

        return orderedDocuments;
    }
}
