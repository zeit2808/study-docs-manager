package com.studydocs.manager.service;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.storage.StorageProvider;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FileCleanupService - Scheduled service for cleaning up deleted document files
 * 
 * This service automatically deletes files from storage after documents have
 * been
 * soft-deleted for a configurable retention period (default: 30 days).
 * 
 * Features:
 * - Runs on configurable schedule (default: daily at 2:00 AM)
 * - Configurable retention period
 * - Can be enabled/disabled via configuration
 * - Errors on individual files don't stop the cleanup process
 * - Comprehensive logging for audit trail
 */
@Service
public class FileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(FileCleanupService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StorageProvider storageProvider;

    @Value("${cleanup.deleted-files.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${cleanup.deleted-files.retention-days:30}")
    private int retentionDays;

    /**
     * Scheduled cleanup job - Runs according to cron expression
     * Default: 0 0 2 * * * (Daily at 2:00 AM)
     * 
     * Process:
     * 1. Check if cleanup is enabled
     * 2. Calculate cutoff date (now - retention period)
     * 3. Find all documents deleted before cutoff with file references
     * 4. Delete each file from storage
     * 5. Log results
     */
    @Scheduled(cron = "${cleanup.deleted-files.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupDeletedDocumentFiles() {
        if (!cleanupEnabled) {
            logger.debug("File cleanup is disabled. Skipping...");
            return;
        }

        logger.info("Starting scheduled cleanup of deleted document files");
        logger.info("Retention period: {} days", retentionDays);

        // Calculate cutoff date
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        logger.info("Cutoff date: {} (documents deleted before this date will have files removed)", cutoffDate);

        // Find eligible documents
        List<Document> documentsToCleanup = documentRepository
                .findByDeletedAtBeforeAndObjectNameIsNotNull(cutoffDate);

        if (documentsToCleanup.isEmpty()) {
            logger.info("No files eligible for cleanup");
            return;
        }

        logger.info("Found {} documents eligible for file cleanup", documentsToCleanup.size());

        // Cleanup statistics
        int successCount = 0;
        int errorCount = 0;

        // Process each document
        for (Document document : documentsToCleanup) {
            try {
                String objectName = document.getObjectName();

                if (objectName == null || objectName.isEmpty()) {
                    continue; // Should not happen due to query, but safety check
                }

                logger.debug("Deleting file: {} for document ID: {}", objectName, document.getId());

                // Delete file from storage
                storageProvider.deleteFile(objectName);

                // Optional: Clear objectName from document to prevent future cleanup attempts
                // document.setObjectName(null);
                // documentRepository.save(document);

                successCount++;
                logger.info("Successfully deleted file: {} for document ID: {} (title: {})",
                        objectName, document.getId(), document.getTitle());

            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to delete file: {} for document ID: {} - Error: {}",
                        document.getObjectName(), document.getId(), e.getMessage(), e);
                // Continue processing other files despite error
            }
        }

        // Final summary
        logger.info("Cleanup completed: {} files deleted successfully, {} errors",
                successCount, errorCount);

        if (errorCount > 0) {
            logger.warn("Some files failed to delete. Check error logs for details.");
        }
    }

    /**
     * Manual cleanup trigger for testing or admin operations
     * Can be called directly or exposed via admin endpoint
     */
    @Transactional
    public CleanupResult manualCleanup() {
        logger.info("Manual cleanup triggered");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<Document> documentsToCleanup = documentRepository
                .findByDeletedAtBeforeAndObjectNameIsNotNull(cutoffDate);

        int successCount = 0;
        int errorCount = 0;

        for (Document document : documentsToCleanup) {
            try {
                if (document.getObjectName() != null && !document.getObjectName().isEmpty()) {
                    storageProvider.deleteFile(document.getObjectName());
                    successCount++;
                }
            } catch (Exception e) {
                errorCount++;
                logger.error("Manual cleanup failed for document {}: {}",
                        document.getId(), e.getMessage());
            }
        }

        return new CleanupResult(documentsToCleanup.size(), successCount, errorCount);
    }

    /**
     * Result object for cleanup operations
     */
    public static class CleanupResult {
        private final int totalDocuments;
        private final int successCount;
        private final int errorCount;

        public CleanupResult(int totalDocuments, int successCount, int errorCount) {
            this.totalDocuments = totalDocuments;
            this.successCount = successCount;
            this.errorCount = errorCount;
        }

        public int getTotalDocuments() {
            return totalDocuments;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }
    }
}
