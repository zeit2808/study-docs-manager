package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.repository.DocumentAssetRepository;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.stream.Collectors;

@Service
public class FileManagerResponseMapper {

    private static final Logger logger = LoggerFactory.getLogger(FileManagerResponseMapper.class);

    private final DocumentRepository documentRepository;
    private final DocumentAssetRepository documentAssetRepository;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final StorageProvider storageProvider;

    public FileManagerResponseMapper(
            DocumentRepository documentRepository,
            DocumentAssetRepository documentAssetRepository,
            FileManagerNamePolicy fileManagerNamePolicy,
            StorageProvider storageProvider) {
        this.documentRepository = documentRepository;
        this.documentAssetRepository = documentAssetRepository;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.storageProvider = storageProvider;
    }

    public FolderResponse toFolderResponse(Folder folder) {
        FolderResponse response = new FolderResponse();
        response.setId(folder.getId());
        response.setName(folder.getName());
        response.setParentId(folder.getParent() != null ? folder.getParent().getId() : null);
        response.setSortOrder(folder.getSortOrder());
        response.setDocumentCount((int) documentRepository.countByFolderIdAndDeletedAtIsNull(folder.getId()));
        response.setCreatedAt(folder.getCreatedAt());
        response.setUpdatedAt(folder.getUpdatedAt());
        return response;
    }

    public DocumentResponse toDocumentResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setUserId(document.getUser().getId());
        response.setUsername(document.getUser().getUsername());
        response.setTitle(document.getTitle());
        response.setDescription(document.getDescription());
        applyAssetFields(response, document);
        response.setDisplayName(fileManagerNamePolicy.requireDocumentName(
                document.getDisplayName(),
                response.getFileName(),
                document.getTitle()));
        response.setStatus(document.getStatus().name());
        response.setVisibility(document.getVisibility().name());
        response.setIsFeatured(document.getIsFeatured());
        response.setFavoriteCount(document.getFavouriteCount());
        response.setRatingAverage(document.getRatingAverage());
        response.setRatingCount(document.getRatingCount());
        response.setVersionNumber(document.getVersionNumber());
        response.setLanguage(document.getLanguage());
        if (document.getFolder() != null) {
            response.setFolderId(document.getFolder().getId());
            response.setFolderName(document.getFolder().getName());
        }
        response.setSubjects(document.getDocumentSubjects().stream()
                .map(ds -> ds.getSubject().getName())
                .collect(Collectors.toSet()));
        response.setTags(document.getDocumentTags().stream()
                .map(dt -> dt.getTag().getName())
                .collect(Collectors.toSet()));
        response.setCreatedAt(document.getCreatedAt());
        if (document.getCreatedBy() != null) {
            response.setCreatedByUsername(document.getCreatedBy().getUsername());
        }
        response.setUpdatedAt(document.getUpdatedAt());
        if (document.getUpdatedBy() != null) {
            response.setUpdatedByUsername(document.getUpdatedBy().getUsername());
        }
        return response;
    }

    public DocumentAsset resolveAsset(Document document) {
        DocumentAsset asset = document.getAsset();
        if (asset == null && document.getId() != null) {
            asset = documentAssetRepository.findByDocumentId(document.getId()).orElse(null);
        }
        return asset;
    }

    public boolean wasFileCleaned(DocumentAsset asset) {
        if (asset == null) {
            return false;
        }
        if (asset.getObjectName() != null && !asset.getObjectName().isBlank()) {
            return false;
        }
        return asset.getFileName() != null
                || asset.getFileType() != null
                || asset.getFileSize() != null
                || asset.getThumbnailObjectName() != null;
    }

    private void applyAssetFields(DocumentResponse response, Document document) {
        DocumentAsset asset = resolveAsset(document);
        if (asset == null) {
            return;
        }

        response.setObjectName(asset.getObjectName());
        response.setFileName(asset.getFileName());
        response.setFileSize(asset.getFileSize());
        response.setFileType(asset.getFileType());
        response.setFileUrl(generateUrl(asset.getObjectName()));
        response.setThumbnailUrl(generateUrl(asset.getThumbnailObjectName()));
    }

    private String generateUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        try {
            return storageProvider.generatePresignedUrl(objectName, 7 * 24 * 60);
        } catch (IOException e) {
            logger.warn("Failed to generate storage URL for object: {}", objectName, e);
            return null;
        }
    }
}
