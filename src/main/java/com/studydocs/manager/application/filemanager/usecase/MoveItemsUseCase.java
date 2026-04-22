package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.enums.ClipboardOperation;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.FileManagerItemType;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerEventService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseFactory;
import com.studydocs.manager.service.filemanager.FileManagerSelection;
import com.studydocs.manager.service.filemanager.FileManagerTreeService;
import com.studydocs.manager.service.folder.FolderEventService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MoveItemsUseCase {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final FileManagerTreeService fileManagerTreeService;
    private final FileManagerEventService fileManagerEventService;
    private final FileManagerResponseFactory fileManagerResponseFactory;
    private final FolderEventService folderEventService;

    public MoveItemsUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            FileManagerTreeService fileManagerTreeService,
            FileManagerEventService fileManagerEventService,
            FileManagerResponseFactory fileManagerResponseFactory,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.fileManagerTreeService = fileManagerTreeService;
        this.fileManagerEventService = fileManagerEventService;
        this.fileManagerResponseFactory = fileManagerResponseFactory;
        this.folderEventService = folderEventService;
    }

    public FileManagerPasteResponse execute(List<FileManagerSelection> selections, Folder targetFolder, User actor) {
        Set<String> occupiedNames = new LinkedHashSet<>(fileManagerNamespaceService.loadNormalizedNames(
                actor.getId(),
                targetFolder != null ? targetFolder.getId() : null));
        List<MovePlan> plans = new ArrayList<>();
        TransferCounters counters = new TransferCounters();

        for (FileManagerSelection selection : selections) {
            if (selection.type() == FileManagerItemType.FOLDER) {
                Folder folder = selection.folder();
                if (fileManagerTreeService.sameFolder(folder.getParent(), targetFolder)) {
                    throw new BadRequestException(
                            "Folder is already in the target location",
                            "FOLDER_ALREADY_IN_TARGET",
                            "targetFolderId");
                }

                String normalizedName = fileManagerNamePolicy.normalize(folder.getName());
                if (occupiedNames.contains(normalizedName)) {
                    throw new ConflictException(
                            "Another item with the same name already exists in the target location",
                            "MOVE_NAME_CONFLICT",
                            "items");
                }
                occupiedNames.add(normalizedName);

                List<Folder> folderTree = fileManagerTreeService.collectActiveFolderTree(folder);
                counters.folderCount += folderTree.size();
                counters.documentCount += fileManagerTreeService.countActiveDocuments(folderTree);
                plans.add(MovePlan.forFolder(folder));
                continue;
            }

            Document document = selection.document();
            if (fileManagerTreeService.sameFolder(document.getFolder(), targetFolder)) {
                throw new BadRequestException(
                        "Document is already in the target location",
                        "DOCUMENT_ALREADY_IN_TARGET",
                        "targetFolderId");
            }

            String documentName = fileManagerNamePolicy.effectiveDocumentName(document);
            String normalizedName = fileManagerNamePolicy.normalize(documentName);
            if (occupiedNames.contains(normalizedName)) {
                throw new ConflictException(
                        "Another item with the same name already exists in the target location",
                        "MOVE_NAME_CONFLICT",
                        "items");
            }
            occupiedNames.add(normalizedName);

            counters.documentCount++;
            plans.add(MovePlan.forDocument(document));
        }

        List<Document> documentsToReindex = new ArrayList<>();
        List<FileManagerPasteResult> results = new ArrayList<>();
        for (MovePlan plan : plans) {
            if (plan.folder() != null) {
                Folder folder = plan.folder();
                folder.setParent(targetFolder);
                Folder saved = folderRepository.save(folder);
                results.add(fileManagerResponseFactory.buildResult(
                        FileManagerItemType.FOLDER,
                        folder.getId(),
                        saved.getId(),
                        saved.getName(),
                        targetFolder));
                // audit_logs: admin oversight (actor, targetFolder, IP)
                fileManagerEventService.logFolderAudit(actor, saved, AuditAction.MOVE_FOLDER, targetFolder);
                // folder_events: history timeline của folder
                folderEventService.logMoved(saved, folder.getParent());
                continue;
            }

            Document document = plan.document();
            document.setFolder(targetFolder);
            document.setUpdatedBy(actor);
            Document saved = documentRepository.save(document);
            results.add(fileManagerResponseFactory.buildResult(
                    FileManagerItemType.DOCUMENT,
                    document.getId(),
                    saved.getId(),
                    fileManagerNamePolicy.effectiveDocumentName(saved),
                    targetFolder));
            // document_events: lifecycle history của document
            fileManagerEventService.logDocumentEvent(saved, DocumentEventType.MOVED, "Document moved");
            // audit_logs: admin oversight (actor, targetFolder, IP)
            fileManagerEventService.logDocumentAudit(actor, saved, AuditAction.MOVE_DOCUMENT, targetFolder);
            if (saved.getStatus() == DocumentStatus.PUBLISHED) {
                documentsToReindex.add(saved);
            }
        }

        documentsToReindex.forEach(fileManagerEventService::indexPublished);
        return fileManagerResponseFactory.buildResponse(
                ClipboardOperation.MOVE,
                targetFolder,
                counters.folderCount,
                counters.documentCount,
                results);
    }

    private record MovePlan(Folder folder, Document document) {
        private static MovePlan forFolder(Folder folder) {
            return new MovePlan(folder, null);
        }

        private static MovePlan forDocument(Document document) {
            return new MovePlan(null, document);
        }
    }

    private static final class TransferCounters {
        private int folderCount;
        private int documentCount;
    }
}
