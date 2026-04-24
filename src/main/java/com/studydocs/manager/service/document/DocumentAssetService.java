package com.studydocs.manager.service.document;


import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.repository.DocumentAssetRepository;
import org.springframework.stereotype.Service;

/**
 * Manages document asset (file metadata) persistence.
 * Extracted from DocumentService to satisfy Single Responsibility Principle.
 */
@Service
public class DocumentAssetService {

    private final DocumentAssetRepository documentAssetRepository;

    public DocumentAssetService(DocumentAssetRepository documentAssetRepository) {
        this.documentAssetRepository = documentAssetRepository;
    }

    /**
     * Creates or updates the asset record for a document.
     */
    public void upsertAsset(Document document, String objectName, String fileName,
            Long fileSize, String fileType, String thumbnailObjectName) {
        DocumentAsset asset = documentAssetRepository.findByDocumentId(document.getId())
                .orElseGet(() -> {
                    DocumentAsset created = new DocumentAsset();
                    created.setDocument(document);
                    return created;
                });

        asset.setObjectName(objectName);
        asset.setFileName(fileName);
        asset.setFileSize(fileSize);
        asset.setFileType(fileType);
        asset.setThumbnailObjectName(thumbnailObjectName);

        DocumentAsset savedAsset = documentAssetRepository.save(asset);
        document.setAsset(savedAsset);
    }

}
