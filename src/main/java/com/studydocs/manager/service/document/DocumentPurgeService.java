package com.studydocs.manager.service.document;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.DocumentVersion;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.DocumentVersionRepository;
import com.studydocs.manager.service.filemanager.FileManagerEventService;
import com.studydocs.manager.storage.StorageProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DocumentPurgeService {

    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileManagerEventService fileManagerEventService;
    private final StorageProvider storageProvider;

    public DocumentPurgeService(
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            DocumentVersionRepository documentVersionRepository,
            FileManagerEventService fileManagerEventService,
            StorageProvider storageProvider) {
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.fileManagerEventService = fileManagerEventService;
        this.storageProvider = storageProvider;
    }

    public void purge(Document document) throws IOException {
        Set<String> objectNames = collectObjectNames(document.getId());
        for (String objectName : objectNames) {
            storageProvider.deleteFile(objectName);
        }

        fileManagerEventService.deleteFromIndex(document.getId());
        documentRepository.delete(document);
    }

    private Set<String> collectObjectNames(Long documentId) {
        Set<String> objectNames = new LinkedHashSet<>();

        DocumentAsset asset = documentAssetRepository.findByDocumentId(documentId).orElse(null);
        if (asset != null) {
            addIfPresent(objectNames, asset.getObjectName());
            addIfPresent(objectNames, asset.getThumbnailObjectName());
        }

        List<DocumentVersion> versions = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
        for (DocumentVersion version : versions) {
            addIfPresent(objectNames, version.getObjectName());
        }

        return objectNames;
    }

    private void addIfPresent(Set<String> objectNames, String objectName) {
        if (objectName != null && !objectName.isBlank()) {
            objectNames.add(objectName);
        }
    }
}
