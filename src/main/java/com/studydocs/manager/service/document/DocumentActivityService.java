package com.studydocs.manager.service.document;

import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.search.DocumentSearchSyncService;
import com.studydocs.manager.security.utils.SecurityUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Handles document side-effects: event logging and search reindexing.
 * Extracted from DocumentService to satisfy Single Responsibility Principle.
 */
@Service
public class DocumentActivityService {

    private final DocumentEventService documentEventService;
    private final DocumentSearchSyncService documentSearchSyncService;
    private final SecurityUtils securityUtils;

    public DocumentActivityService(
            ObjectProvider<DocumentEventService> documentEventServiceProvider,
            ObjectProvider<DocumentSearchSyncService> documentSearchSyncServiceProvider,
            SecurityUtils securityUtils) {
        this.documentEventService = documentEventServiceProvider.getIfAvailable();
        this.documentSearchSyncService = documentSearchSyncServiceProvider.getIfAvailable();
        this.securityUtils = securityUtils;
    }

    /**
     * Logs a document event with current user context (IP, user-agent).
     */
    public void logEvent(Document document, DocumentEventType eventType, String description) {
        if (documentEventService == null) {
            return;
        }
        Long userId = securityUtils.getCurrentUserId();
        String ip = securityUtils.getClientIp();
        String userAgent = securityUtils.getUserAgent();
        documentEventService.logEvent(
                document.getId(),
                userId,
                eventType,
                description,
                null,
                null,
                ip,
                userAgent);
    }

    /**
     * Schedules a search reindex for the given document after transaction commit.
     */
    public void scheduleReindex(Long documentId) {
        if (documentSearchSyncService != null) {
            documentSearchSyncService.scheduleReindex(documentId);
        }
    }
}
