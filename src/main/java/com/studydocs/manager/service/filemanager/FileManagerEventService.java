package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.AuditAction;
import com.studydocs.manager.enums.DocumentEventType;
import com.studydocs.manager.search.DocumentSearchSyncService;
import com.studydocs.manager.security.utils.SecurityUtils;
import com.studydocs.manager.service.document.AuditLogService;
import com.studydocs.manager.service.document.DocumentEventService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class FileManagerEventService {

    private final SecurityUtils securityUtils;
    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final DocumentEventService documentEventService;
    private final DocumentSearchSyncService documentSearchSyncService;
    private final AuditLogService auditLogService;

    public FileManagerEventService(
            SecurityUtils securityUtils,
            FileManagerNamePolicy fileManagerNamePolicy,
            ObjectProvider<DocumentEventService> documentEventServiceProvider,
            ObjectProvider<DocumentSearchSyncService> documentSearchSyncServiceProvider,
            ObjectProvider<AuditLogService> auditLogServiceProvider) {
        this.securityUtils = securityUtils;
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.documentEventService = documentEventServiceProvider.getIfAvailable();
        this.documentSearchSyncService = documentSearchSyncServiceProvider.getIfAvailable();
        this.auditLogService = auditLogServiceProvider.getIfAvailable();
    }

    public void deleteFromIndex(Long documentId) {
        if (documentSearchSyncService != null) {
            documentSearchSyncService.scheduleDelete(documentId);
        }
    }

    public void indexPublished(Document document) {
        if (documentSearchSyncService != null && document != null && document.getId() != null) {
            documentSearchSyncService.scheduleReindex(document.getId());
        }
    }

    public void logDocumentEvent(Document document, DocumentEventType eventType, String description) {
        if (documentEventService == null) {
            return;
        }

        documentEventService.logEvent(
                document.getId(),
                securityUtils.getCurrentUserId(),
                eventType,
                description,
                null,
                null,
                securityUtils.getClientIp(),
                securityUtils.getUserAgent());
    }

    public void logDocumentAudit(User actor, Document document, AuditAction action, Folder targetFolder) {
        if (auditLogService == null) {
            return;
        }

        String details = "documentId=" + document.getId()
                + ",name=" + fileManagerNamePolicy.effectiveDocumentName(document)
                + ",targetFolderId=" + (targetFolder != null ? targetFolder.getId() : "root");
        auditLogService.log(
                actor.getId(),
                document.getUser().getId(),
                action,
                details,
                securityUtils.getClientIp(),
                securityUtils.getUserAgent());
    }

    public void logFolderAudit(User actor, Folder folder, AuditAction action, Folder targetFolder) {
        if (auditLogService == null) {
            return;
        }

        String details = "folderId=" + folder.getId()
                + ",name=" + folder.getName()
                + ",targetFolderId=" + (targetFolder != null ? targetFolder.getId() : "root");
        auditLogService.log(
                actor.getId(),
                folder.getUser().getId(),
                action,
                details,
                securityUtils.getClientIp(),
                securityUtils.getUserAgent());
    }
}
