package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.config.StorageProperties;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerPasteResult;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.DocumentSubject;
import com.studydocs.manager.entity.DocumentTag;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.enums.ClipboardOperation;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.enums.FileManagerItemType;
import com.studydocs.manager.exception.ServiceUnavailableException;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.service.filemanager.FileManagerAssetStateService;
import com.studydocs.manager.service.filemanager.FileManagerEventService;
import com.studydocs.manager.service.filemanager.FileManagerNamePolicy;
import com.studydocs.manager.service.filemanager.FileManagerNamespaceService;
import com.studydocs.manager.service.filemanager.FileManagerResponseFactory;
import com.studydocs.manager.service.filemanager.FileManagerSelection;
import com.studydocs.manager.service.folder.FolderEventService;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CopyItemsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CopyItemsUseCase.class);

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;
    private final StorageProvider storageProvider;
    private final StorageProperties storageProperties;
    private final FileManagerAssetStateService fileManagerAssetStateService;
    private final FileManagerEventService fileManagerEventService;
    private final FileManagerResponseFactory fileManagerResponseFactory;
    private final FolderEventService folderEventService;

    public CopyItemsUseCase(
            FolderRepository folderRepository,
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService,
            StorageProvider storageProvider,
            StorageProperties storageProperties,
            FileManagerAssetStateService fileManagerAssetStateService,
            FileManagerEventService fileManagerEventService,
            FileManagerResponseFactory fileManagerResponseFactory,
            FolderEventService folderEventService) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
        this.storageProvider = storageProvider;
        this.storageProperties = storageProperties;
        this.fileManagerAssetStateService = fileManagerAssetStateService;
        this.fileManagerEventService = fileManagerEventService;
        this.fileManagerResponseFactory = fileManagerResponseFactory;
        this.folderEventService = folderEventService;
    }

    public FileManagerPasteResponse execute(List<FileManagerSelection> selections, Folder targetFolder, User actor) {
        Set<String> occupiedNames = new LinkedHashSet<>(fileManagerNamespaceService.loadNormalizedNames(
                actor.getId(),
                targetFolder != null ? targetFolder.getId() : null));
        List<CopyPlan> plans = new ArrayList<>();

        for (FileManagerSelection selection : selections) {
            String preferredName = selection.type() == FileManagerItemType.FOLDER
                    ? selection.folder().getName()
                    : fileManagerNamePolicy.effectiveDocumentName(selection.document());
            String finalName = fileManagerNamePolicy.resolveCopyName(preferredName, occupiedNames);
            occupiedNames.add(fileManagerNamePolicy.normalize(finalName));
            plans.add(new CopyPlan(selection, finalName));
        }

        StorageCopyTracker storageCopyTracker = new StorageCopyTracker();
        TransferCounters counters = new TransferCounters();
        List<FileManagerPasteResult> results = new ArrayList<>();
        try {
            for (CopyPlan plan : plans) {
                if (plan.selection().type() == FileManagerItemType.FOLDER) {
                    Folder copiedRoot = cloneFolderTree(
                            plan.selection().folder(),
                            targetFolder,
                            plan.finalName(),
                            actor,
                            storageCopyTracker,
                            counters);
                    results.add(fileManagerResponseFactory.buildResult(
                            FileManagerItemType.FOLDER,
                            plan.selection().folder().getId(),
                            copiedRoot.getId(),
                            copiedRoot.getName(),
                            targetFolder));
                    // audit_logs: admin oversight (actor, targetFolder, IP)
                    fileManagerEventService.logFolderAudit(actor, copiedRoot, AuditAction.COPY_FOLDER, targetFolder);
                    // folder_events: history timeline của folder bản sao
                    folderEventService.logCopied(copiedRoot, plan.selection().folder());
                    continue;
                }

                Document copiedDocument = cloneDocument(
                        plan.selection().document(),
                        targetFolder,
                        plan.finalName(),
                        actor,
                        storageCopyTracker);
                counters.documentCount++;
                results.add(fileManagerResponseFactory.buildResult(
                        FileManagerItemType.DOCUMENT,
                        plan.selection().document().getId(),
                        copiedDocument.getId(),
                        fileManagerNamePolicy.effectiveDocumentName(copiedDocument),
                        targetFolder));
                fileManagerEventService.logDocumentAudit(actor, copiedDocument, AuditAction.COPY_DOCUMENT, targetFolder);
            }
        } catch (RuntimeException ex) {
            storageCopyTracker.cleanup(storageProvider);
            throw ex;
        }

        return fileManagerResponseFactory.buildResponse(
                ClipboardOperation.COPY,
                targetFolder,
                counters.folderCount,
                counters.documentCount,
                results);
    }

    private Folder cloneFolderTree(
            Folder sourceFolder,
            Folder targetParent,
            String finalName,
            User actor,
            StorageCopyTracker storageCopyTracker,
            TransferCounters counters) {
        Folder clone = new Folder();
        clone.setUser(actor);
        clone.setParent(targetParent);
        clone.setName(finalName);
        clone.setSortOrder(sourceFolder.getSortOrder());
        Folder savedFolder = folderRepository.save(clone);
        counters.folderCount++;

        List<Folder> childFolders = folderRepository.findByParentIdAndDeletedAtIsNullOrderBySortOrder(sourceFolder.getId());
        List<Document> childDocuments = documentRepository.findAllByFolderIdAndDeletedAtIsNull(sourceFolder.getId()).stream()
                .sorted(Comparator.comparing(Document::getId))
                .toList();

        Set<String> occupiedNames = new LinkedHashSet<>();
        for (Folder childFolder : childFolders) {
            String childName = fileManagerNamePolicy.resolveCopyName(childFolder.getName(), occupiedNames);
            occupiedNames.add(fileManagerNamePolicy.normalize(childName));
            cloneFolderTree(childFolder, savedFolder, childName, actor, storageCopyTracker, counters);
        }

        for (Document childDocument : childDocuments) {
            String preferredName = fileManagerNamePolicy.effectiveDocumentName(childDocument);
            String finalDocumentName = fileManagerNamePolicy.resolveCopyName(preferredName, occupiedNames);
            occupiedNames.add(fileManagerNamePolicy.normalize(finalDocumentName));
            cloneDocument(childDocument, savedFolder, finalDocumentName, actor, storageCopyTracker);
            counters.documentCount++;
        }

        return savedFolder;
    }

    private Document cloneDocument(
            Document sourceDocument,
            Folder targetFolder,
            String finalName,
            User actor,
            StorageCopyTracker storageCopyTracker) {
        Document clone = new Document();
        clone.setUser(actor);
        clone.setTitle(sourceDocument.getTitle());
        clone.setDescription(sourceDocument.getDescription());
        clone.setDisplayName(finalName);
        clone.setStatus(DocumentStatus.DRAFT);
        clone.setVisibility(DocumentVisibility.PRIVATE);
        clone.setIsFeatured(Boolean.FALSE);
        clone.setFavouriteCount(0);
        clone.setRatingAverage(BigDecimal.ZERO);
        clone.setRatingCount(0);
        clone.setVersionNumber(1);
        clone.setLanguage(sourceDocument.getLanguage());
        clone.setFolder(targetFolder);
        clone.setCreatedBy(actor);

        for (DocumentSubject sourceSubject : sourceDocument.getDocumentSubjects()) {
            DocumentSubject copiedSubject = new DocumentSubject();
            copiedSubject.setDocument(clone);
            copiedSubject.setSubject(sourceSubject.getSubject());
            clone.getDocumentSubjects().add(copiedSubject);
        }

        Set<Long> seenTagIds = new HashSet<>();
        for (DocumentTag sourceTag : sourceDocument.getDocumentTags()) {
            Tag tag = sourceTag.getTag();
            if (tag == null || tag.getId() == null || !seenTagIds.add(tag.getId())) {
                continue;
            }
            DocumentTag copiedTag = new DocumentTag();
            copiedTag.setDocument(clone);
            copiedTag.setTag(tag);
            clone.getDocumentTags().add(copiedTag);
        }

        Document saved = documentRepository.save(clone);

        DocumentAsset copiedAsset = copyAsset(sourceDocument, finalName, storageCopyTracker);
        if (copiedAsset != null) {
            copiedAsset.setDocument(saved);
            DocumentAsset savedAsset = documentAssetRepository.save(copiedAsset);
            saved.setAsset(savedAsset);
        }

        fileManagerEventService.logDocumentEvent(saved, DocumentEventType.COPIED, "Document copied");
        return saved;
    }

    private DocumentAsset copyAsset(Document sourceDocument, String finalName, StorageCopyTracker storageCopyTracker) {
        DocumentAsset sourceAsset = fileManagerAssetStateService.resolveAsset(sourceDocument);
        if (sourceAsset == null) {
            return null;
        }

        DocumentAsset copiedAsset = new DocumentAsset();
        copiedAsset.setFileName(sourceAsset.getFileName());
        copiedAsset.setFileSize(sourceAsset.getFileSize());
        copiedAsset.setFileType(sourceAsset.getFileType());
        copiedAsset.setThumbnailObjectName(sourceAsset.getThumbnailObjectName());

        if (sourceAsset.getObjectName() != null && !sourceAsset.getObjectName().isBlank()) {
            try {
                String preferredFilename = sourceAsset.getFileName() != null && !sourceAsset.getFileName().isBlank()
                        ? sourceAsset.getFileName()
                        : finalName;
                String copiedObjectName = storageProvider.copyFile(
                        sourceAsset.getObjectName(),
                        storageProperties.getDocumentsFolder(),
                        preferredFilename);
                storageCopyTracker.track(copiedObjectName);
                copiedAsset.setObjectName(copiedObjectName);
            } catch (IOException e) {
                throw new ServiceUnavailableException(
                        "Failed to copy document file from storage",
                        "DOCUMENT_COPY_STORAGE_FAILED",
                        "items");
            }
        } else {
            copiedAsset.setObjectName(null);
        }

        return copiedAsset;
    }

    private record CopyPlan(FileManagerSelection selection, String finalName) {
    }

    private static final class TransferCounters {
        private int folderCount;
        private int documentCount;
    }

    private static final class StorageCopyTracker {
        private final List<String> createdObjectNames = new ArrayList<>();

        private void track(String objectName) {
            createdObjectNames.add(objectName);
        }

        private void cleanup(StorageProvider storageProvider) {
            for (int i = createdObjectNames.size() - 1; i >= 0; i--) {
                String objectName = createdObjectNames.get(i);
                try {
                    storageProvider.deleteFile(objectName);
                } catch (IOException e) {
                    logger.warn("Failed to clean copied object after rollback: {}", objectName, e);
                }
            }
        }
    }
}
