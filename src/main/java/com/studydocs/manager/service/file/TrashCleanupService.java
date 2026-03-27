package com.studydocs.manager.service.file;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.repository.DocumentRepository;
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

/**
 * TrashCleanupService — Tự động hard-delete DB records của documents
 * đã bị soft-delete quá thời gian lưu giữ (trash.retention-days).
 *
 * Phân biệt với FileCleanupService:
 * - FileCleanupService: xóa FILE vật lý trên MinIO sau 30 ngày
 * - TrashCleanupService: xóa RECORD trong DB sau khi file đã được dọn
 * (objectName=null) VÀ quá thời gian lưu giữ
 *
 * Chỉ hard-delete records mà file đã được FileCleanupService dọn sạch
 * (objectName IS NULL) để đảm bảo không leak file trên MinIO.
 */
@Service
public class TrashCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TrashCleanupService.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${cleanup.trash.enabled:true}")
    private boolean trashCleanupEnabled;

    @Value("${cleanup.trash.retention-days:90}")
    private int trashRetentionDays;

    /**
     * Chạy lúc 3:00 AM mỗi ngày (1 tiếng sau FileCleanupService).
     * Tìm documents thỏa mãn:
     * - status = DELETED
     * - deletedAt < now - trashRetentionDays
     * - objectName IS NULL (file đã được FileCleanupService dọn rồi)
     *
     * Hard-delete toàn bộ records đó khỏi DB.
     */
    @Scheduled(cron = "${cleanup.trash.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTrash() {
        if (!trashCleanupEnabled) {
            logger.debug("Trash cleanup is disabled. Skipping...");
            return;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(trashRetentionDays);
        logger.info("Starting trash purge: records with deletedAt before {} and no file reference", cutoffDate);

        int batchSize = 200;
        int totalPurged = 0;
        Pageable pageable = PageRequest.of(0, batchSize);
        Page<Document> page;

        do {
            // Chỉ hard-delete records ĐÃ được FileCleanupService dọn file (objectName=null)
            // Tránh trường hợp hard-delete record nhưng file vẫn còn trên MinIO → file rác
            page = documentRepository.findByStatusAndDeletedAtBeforeAndObjectNameIsNull(
                    Document.DocumentStatus.DELETED, cutoffDate, pageable);

            if (page.isEmpty())
                break;

            logger.info("Purging batch of {} expired trash records", page.getNumberOfElements());
            documentRepository.deleteAll(page.getContent());
            totalPurged += page.getNumberOfElements();

            // page 0 luôn refresh vì records đã delete rớt khỏi kết quả
        } while (page.hasContent());

        logger.info("Trash purge completed: {} records permanently deleted from DB", totalPurged);
    }
}
