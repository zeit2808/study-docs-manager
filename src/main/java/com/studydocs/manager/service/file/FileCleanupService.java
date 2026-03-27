package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.storage.StorageProvider;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        int batchSize = 100;
        int totalSuccess = 0;
        int totalError = 0;

        Page<Document> page;
        Pageable pageable = PageRequest.of(0, batchSize);

        do {
            page = documentRepository
                    .findByStatusAndDeletedAtBeforeAndObjectNameIsNotNull(
                            Document.DocumentStatus.DELETED, cutoffDate, pageable);

            if (page.isEmpty())
                break;

            logger.info("Processing batch of {} documents", page.getNumberOfElements());

            for (Document document : page.getContent()) {
                try {
                    String objectName = document.getObjectName();
                    if (objectName == null || objectName.isEmpty())
                        continue;

                    logger.debug("Deleting file: {} for document ID: {}", objectName, document.getId());

                    // Bước 1: Xóa file vật lý khỏi MinIO
                    storageProvider.deleteFile(objectName);

                    // Bước 2: Null objectName/fileUrl để không cleanup lại lần sau
                    // GIỮ NGUYÊN record DB với status=DELETED → user có thể restore metadata
                    // (File đã mất nhưng title, description, tags... vẫn còn trong Trash)
                    document.setObjectName(null);
                    document.setFileUrl(null);
                    documentRepository.save(document);

                    totalSuccess++;
                    logger.info("Cleaned up file for document ID: {} (file: {})", document.getId(), objectName);

                } catch (Exception e) {
                    totalError++;
                    logger.error("Failed to clean up document ID: {} - Error: {}",
                            document.getId(), e.getMessage(), e);
                    // Tiếp tục xử lý các documents còn lại dù có lỗi
                }
            }

            // Luôn query page 0: documents đã hard-delete sẽ tự rớt khỏi kết quả
            // → page 0 liên tục trượt sang batch tiếp theo
        } while (page.hasContent());

        logger.info("Cleanup completed: {} documents cleaned up, {} errors", totalSuccess, totalError);
        if (totalError > 0) {
            logger.warn("Failed documents will be retried in the next scheduled run.");
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
                    // Giữ record, chỉ null file reference
                    document.setObjectName(null);
                    document.setFileUrl(null);
                    documentRepository.save(document);
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
