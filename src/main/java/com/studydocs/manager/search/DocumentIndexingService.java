package com.studydocs.manager.search;

import com.studydocs.manager.config.SearchProperties;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentAsset;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.file.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "search.indexing.enabled", havingValue = "true", matchIfMissing = false)
public class DocumentIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexingService.class);
    private static final int BULK_INDEX_PAGE_SIZE = 50;

    private final DocumentSearchRepository searchRepository;
    private final DocumentRepository documentRepository;
    private final TikaMetadataService tikaMetadataService;
    private final StorageProvider storageProvider;
    private final SearchProperties searchProperties;

    public DocumentIndexingService(DocumentSearchRepository searchRepository,
            DocumentRepository documentRepository,
            TikaMetadataService tikaMetadataService,
            StorageProvider storageProvider,
            SearchProperties searchProperties) {
        this.searchRepository = searchRepository;
        this.documentRepository = documentRepository;
        this.tikaMetadataService = tikaMetadataService;
        this.storageProvider = storageProvider;
        this.searchProperties = searchProperties;
    }

    @Async("searchIndexingExecutor")
    public CompletableFuture<Boolean> updateIndexById(Long documentId) {
        return CompletableFuture.completedFuture(indexDocumentInternal(documentId));
    }

    @Async("searchIndexingExecutor")
    public CompletableFuture<Boolean> deleteFromIndex(Long documentId) {
        try {
            if (searchRepository.existsById(documentId)) {
                searchRepository.deleteById(documentId);
                logger.info("Deleted document {} from search index", documentId);
                return CompletableFuture.completedFuture(true);
            }

            logger.debug("Document {} not found in search index", documentId);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            logger.error("Failed to delete document {} from index: {}", documentId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean reindexDocumentNow(Long documentId) {
        return indexDocumentInternal(documentId);
    }

    public long countIndexedDocuments() {
        return searchRepository.count();
    }

    public int bulkIndexAllDocuments() {
        logger.info("Starting bulk indexing of public published documents");

        int page = 0;
        int totalIndexed = 0;

        while (true) {
            Page<Document> documentsPage = documentRepository.findByStatusAndDeletedAtIsNull(
                    DocumentStatus.PUBLISHED,
                    PageRequest.of(page, BULK_INDEX_PAGE_SIZE));

            if (documentsPage.isEmpty()) {
                break;
            }

            for (Document document : documentsPage.getContent()) {
                if (indexDocumentInternal(document.getId())) {
                    totalIndexed++;
                }
            }

            if (!documentsPage.hasNext()) {
                break;
            }

            page++;
        }

        logger.info("Bulk indexing completed. Indexed {} documents", totalIndexed);
        return totalIndexed;
    }

    public int reindexByAuthor(Long userId) {
        logger.info("Re-indexing public published documents for user {}", userId);

        List<Document> documents = documentRepository.findByUserIdAndStatus(userId, DocumentStatus.PUBLISHED);
        int indexed = 0;
        for (Document document : documents) {
            if (indexDocumentInternal(document.getId())) {
                indexed++;
            }
        }

        logger.info("Re-indexed {} documents for user {}", indexed, userId);
        return indexed;
    }

    private boolean indexDocumentInternal(Long documentId) {
        try {
            Optional<Document> optionalDocument = documentRepository.findByIdForSearchIndexing(documentId);
            if (optionalDocument.isEmpty()) {
                searchRepository.deleteById(documentId);
                logger.debug("Document {} no longer exists, removed stale index entry", documentId);
                return false;
            }

            Document document = optionalDocument.get();
            if (!shouldBeIndexed(document)) {
                searchRepository.deleteById(documentId);
                logger.debug("Skipping index for document {} because it is not public and published", documentId);
                return false;
            }

            DocumentSearchIndex searchIndex = convertToSearchIndex(document);
            searchRepository.save(searchIndex);
            logger.info("Indexed document {}", documentId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to index document {}: {}", documentId, e.getMessage(), e);
            return false;
        }
    }

    private boolean shouldBeIndexed(Document document) {
        return document.getDeletedAt() == null
                && document.getStatus() == DocumentStatus.PUBLISHED
                && document.getVisibility() == DocumentVisibility.PUBLIC;
    }

    private DocumentSearchIndex convertToSearchIndex(Document document) {
        DocumentSearchIndex searchIndex = new DocumentSearchIndex();

        searchIndex.setId(document.getId());
        searchIndex.setTitle(document.getTitle());
        searchIndex.setDescription(document.getDescription());
        searchIndex.setContent(extractContentFromFile(document));

        DocumentAsset asset = document.getAsset();
        if (asset != null) {
            searchIndex.setFileName(asset.getFileName());
            searchIndex.setFileType(asset.getFileType());
            searchIndex.setFileSize(asset.getFileSize());
            searchIndex.setObjectName(asset.getObjectName());
            searchIndex.setThumbnailObjectName(asset.getThumbnailObjectName());
        }

        if (document.getUser() != null) {
            searchIndex.setAuthorId(document.getUser().getId());
            searchIndex.setAuthorName(document.getUser().getFullname());
            searchIndex.setAuthorUsername(document.getUser().getUsername());
        }

        if (document.getDocumentTags() != null) {
            searchIndex.setTags(document.getDocumentTags().stream()
                    .map(dt -> dt.getTag().getName())
                    .collect(Collectors.toList()));
        }

        if (document.getDocumentSubjects() != null) {
            searchIndex.setSubjectIds(document.getDocumentSubjects().stream()
                    .map(ds -> ds.getSubject().getId())
                    .collect(Collectors.toList()));
            searchIndex.setSubjectNames(document.getDocumentSubjects().stream()
                    .map(ds -> ds.getSubject().getName())
                    .collect(Collectors.toList()));
        }

        if (document.getFolder() != null) {
            searchIndex.setFolderId(document.getFolder().getId());
            searchIndex.setFolderName(document.getFolder().getName());
        }

        searchIndex.setStatus(document.getStatus());
        searchIndex.setVisibility(document.getVisibility());
        searchIndex.setIsFeatured(document.getIsFeatured());
        searchIndex.setLanguage(document.getLanguage());
        searchIndex.setFavouriteCount(document.getFavouriteCount());
        searchIndex.setRatingAverage(
                document.getRatingAverage() != null ? document.getRatingAverage().doubleValue() : null);
        searchIndex.setRatingCount(document.getRatingCount());
        searchIndex.setCreatedAt(document.getCreatedAt() != null
                ? document.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null);
        searchIndex.setUpdatedAt(document.getUpdatedAt() != null
                ? document.getUpdatedAt().toInstant(ZoneOffset.UTC)
                : null);
        searchIndex.setIndexedAt(Instant.now());

        return searchIndex;
    }

    private String extractContentFromFile(Document document) {
        DocumentAsset asset = document.getAsset();
        if (asset == null || asset.getObjectName() == null || asset.getObjectName().isBlank()) {
            return null;
        }

        try {
            String decodedObjectName = URLDecoder.decode(asset.getObjectName(), StandardCharsets.UTF_8);
            try (InputStream fileStream = storageProvider.downloadFileAsStream(decodedObjectName)) {
                if (fileStream == null) {
                    logger.warn("Could not download file {} from storage", decodedObjectName);
                    return null;
                }

                String extractedText = tikaMetadataService.extractText(
                        fileStream,
                        searchProperties.getFullTextMaxLength(),
                        asset.getFileName());

                if (extractedText == null || extractedText.isBlank()) {
                    return null;
                }

                return extractedText.trim();
            }
        } catch (Exception e) {
            logger.error("Failed to extract content from file {} for document {}: {}",
                    asset.getObjectName(), document.getId(), e.getMessage(), e);
            return null;
        }
    }
}
