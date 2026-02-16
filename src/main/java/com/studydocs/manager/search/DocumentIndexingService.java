package com.studydocs.manager.search;

import com.studydocs.manager.entity.*;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.service.TikaMetadataService;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service để index và đồng bộ documents vào Elasticsearch
 * 
 * Strategy: Chỉ index documents có status = PUBLISHED
 * Content limit: 10,000 characters preview
 */
@Service
public class DocumentIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexingService.class);
    private static final int CONTENT_LIMIT = 10000;
    private static final int BULK_INDEX_PAGE_SIZE = 50;

    @Autowired
    private DocumentSearchRepository searchRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TikaMetadataService tikaMetadataService;

    @Autowired
    private StorageProvider storageProvider;

    /**
     * Index một document vào Elasticsearch
     * Chỉ index nếu status = PUBLISHED
     * 
     * @param document Document entity to index
     */
    @Async
    public CompletableFuture<Boolean> indexDocument(Document document) {
        try {
            // Chỉ index documents có status PUBLISHED
            if (document.getStatus() != Document.DocumentStatus.PUBLISHED) {
                logger.debug("Skipping indexing for document {} - status is {}",
                        document.getId(), document.getStatus());
                return CompletableFuture.completedFuture(false);
            }

            logger.info("Indexing document: {} - {}", document.getId(), document.getTitle());

            DocumentSearchIndex searchIndex = convertToSearchIndex(document);
            searchRepository.save(searchIndex);

            logger.info("Successfully indexed document: {}", document.getId());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            logger.error("Failed to index document {}: {}", document.getId(), e.getMessage(), e);
            // Don't throw - indexing failure shouldn't block document operations
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Update index cho một document đã tồn tại
     * 
     * @param document Document entity to update
     */
    @Async
    public CompletableFuture<Boolean> updateIndex(Document document) {
        try {
            // Nếu không phải PUBLISHED, xóa khỏi index (nếu có)
            if (document.getStatus() != Document.DocumentStatus.PUBLISHED) {
                logger.info("Document {} is no longer PUBLISHED, removing from index", document.getId());
                return deleteFromIndex(document.getId());
            }

            // Nếu là PUBLISHED, update/create index
            return indexDocument(document);

        } catch (Exception e) {
            logger.error("Failed to update index for document {}: {}", document.getId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Xóa document khỏi Elasticsearch index
     * 
     * @param documentId ID của document cần xóa
     */
    @Async
    public CompletableFuture<Boolean> deleteFromIndex(Long documentId) {
        try {
            if (searchRepository.existsById(documentId)) {
                searchRepository.deleteById(documentId);
                logger.info("Deleted document {} from search index", documentId);
                return CompletableFuture.completedFuture(true);
            } else {
                logger.debug("Document {} not found in search index", documentId);
                return CompletableFuture.completedFuture(false);
            }
        } catch (Exception e) {
            logger.error("Failed to delete document {} from index: {}", documentId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Bulk re-index tất cả documents
     * Chỉ index documents có status = PUBLISHED
     * 
     * @return Số lượng documents đã index thành công
     */
    public int bulkIndexAllDocuments() {
        logger.info("Starting bulk indexing of all PUBLISHED documents...");

        int page = 0;
        int totalIndexed = 0;
        int failedCount = 0;

        while (true) {
            Page<Document> documentsPage = documentRepository.findByStatusAndDeletedAtIsNull(
                    Document.DocumentStatus.PUBLISHED,
                    PageRequest.of(page, BULK_INDEX_PAGE_SIZE));

            if (documentsPage.isEmpty()) {
                break;
            }

            logger.info("Processing page {} with {} documents", page, documentsPage.getNumberOfElements());

            for (Document document : documentsPage.getContent()) {
                try {
                    DocumentSearchIndex searchIndex = convertToSearchIndex(document);
                    searchRepository.save(searchIndex);
                    totalIndexed++;
                } catch (Exception e) {
                    logger.error("Failed to index document {}: {}", document.getId(), e.getMessage());
                    failedCount++;
                }
            }

            page++;

            if (!documentsPage.hasNext()) {
                break;
            }
        }

        logger.info("Bulk indexing completed. Indexed: {}, Failed: {}", totalIndexed, failedCount);
        return totalIndexed;
    }

    /**
     * Re-index documents cho một specific author
     */
    public int reindexByAuthor(Long userId) {
        logger.info("Re-indexing documents for user {}", userId);

        List<Document> documents = documentRepository.findByUserIdAndStatus(
                userId,
                Document.DocumentStatus.PUBLISHED);

        int indexed = 0;
        for (Document document : documents) {
            try {
                DocumentSearchIndex searchIndex = convertToSearchIndex(document);
                searchRepository.save(searchIndex);
                indexed++;
            } catch (Exception e) {
                logger.error("Failed to index document {}: {}", document.getId(), e.getMessage());
            }
        }

        logger.info("Re-indexed {} documents for user {}", indexed, userId);
        return indexed;
    }

    /**
     * Convert Document entity sang DocumentSearchIndex
     * Extract text content từ MinIO file nếu có
     */
    private DocumentSearchIndex convertToSearchIndex(Document document) {
        DocumentSearchIndex searchIndex = new DocumentSearchIndex();

        searchIndex.setId(document.getId());
        searchIndex.setTitle(document.getTitle());
        searchIndex.setDescription(document.getDescription());

        // Extract content từ file nếu có objectName
        String extractedContent = extractContentFromFile(document);
        searchIndex.setContent(extractedContent); // setContent tự động limit 10,000 chars

        // File info
        searchIndex.setFileName(document.getFileName());
        searchIndex.setFileType(document.getFileType());
        searchIndex.setFileSize(document.getFileSize());
        searchIndex.setObjectName(document.getObjectName());

        // Author info
        if (document.getUser() != null) {
            searchIndex.setAuthorId(document.getUser().getId());
            searchIndex.setAuthorName(document.getUser().getFullname());
            searchIndex.setAuthorUsername(document.getUser().getUsername());
        }

        // Tags
        if (document.getDocumentTags() != null) {
            List<String> tags = document.getDocumentTags().stream()
                    .map(dt -> dt.getTag().getName())
                    .collect(Collectors.toList());
            searchIndex.setTags(tags);
        }

        // Subjects
        if (document.getDocumentSubjects() != null) {
            List<Long> subjectIds = document.getDocumentSubjects().stream()
                    .map(ds -> ds.getSubject().getId())
                    .collect(Collectors.toList());
            List<String> subjectNames = document.getDocumentSubjects().stream()
                    .map(ds -> ds.getSubject().getName())
                    .collect(Collectors.toList());
            searchIndex.setSubjectIds(subjectIds);
            searchIndex.setSubjectNames(subjectNames);
        }

        // Folder
        if (document.getFolder() != null) {
            searchIndex.setFolderId(document.getFolder().getId());
            searchIndex.setFolderName(document.getFolder().getName());
        }

        // Status & visibility
        searchIndex.setStatus(document.getStatus());
        searchIndex.setVisibility(document.getVisibility());
        searchIndex.setIsFeatured(document.getIsFeatured());
        searchIndex.setLanguage(document.getLanguage());

        // Statistics
        searchIndex.setViewCount(document.getViewCount());
        searchIndex.setDownloadCount(document.getDownloadCount());
        searchIndex.setFavouriteCount(document.getFavouriteCount());
        searchIndex.setRatingAverage(
                document.getRatingAverage() != null ? document.getRatingAverage().doubleValue() : null);
        searchIndex.setRatingCount(document.getRatingCount());

        // Dates - Convert LocalDateTime to Instant for Elasticsearch
        searchIndex.setCreatedAt(document.getCreatedAt() != null
                ? document.getCreatedAt().toInstant(ZoneOffset.UTC)
                : null);
        searchIndex.setUpdatedAt(document.getUpdatedAt() != null
                ? document.getUpdatedAt().toInstant(ZoneOffset.UTC)
                : null);
        searchIndex.setIndexedAt(Instant.now());

        // Thumbnail
        searchIndex.setThumbnailUrl(document.getThumbnailUrl());

        return searchIndex;
    }

    /**
     * Extract text content từ file stored in MinIO
     * Limit to CONTENT_LIMIT characters
     */
    private String extractContentFromFile(Document document) {
        if (document.getObjectName() == null || document.getObjectName().isEmpty()) {
            logger.debug("No file to extract content from for document {}", document.getId());
            return null;
        }

        logger.debug("Extracting text content from file: {}", document.getObjectName());

        try {
            // Decode URL-encoded objectName (e.g., %20 -> space, %E1%BA%A1i -> Vietnamese
            // chars)
            String decodedObjectName = java.net.URLDecoder.decode(document.getObjectName(), "UTF-8");
            logger.debug("Decoded objectName: {} -> {}", document.getObjectName(), decodedObjectName);

            InputStream fileStream = storageProvider.downloadFileAsStream(decodedObjectName);

            if (fileStream == null) {
                logger.warn("Could not download file {} from storage", decodedObjectName);
                return null;
            }

            String extractedText = tikaMetadataService.extractText(
                    fileStream,
                    CONTENT_LIMIT,
                    document.getFileName() // giúp AutoDetectParser đoán loại file tốt hơn
            );

            if (extractedText != null && !extractedText.trim().isEmpty()) {
                logger.debug("Extracted {} characters of text from {}",
                        extractedText.length(), document.getFileName());
                fileStream.close(); // Ensure stream is closed before returning
                return extractedText.trim();
            }

            fileStream.close();
            return null;

        } catch (Exception e) {
            logger.error("Failed to extract content from file {} for document {}: {}",
                    document.getObjectName(), document.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Simple MockMultipartFile implementation for internal use
     */
    private static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final InputStream inputStream;

        public MockMultipartFile(String name, String originalFilename, String contentType, InputStream inputStream) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            try {
                return inputStream.readAllBytes();
            } catch (Exception e) {
                return new byte[0];
            }
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public void transferTo(java.io.File dest) throws IllegalStateException {
            throw new UnsupportedOperationException();
        }
    }
}
