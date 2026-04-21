package com.studydocs.manager.search;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DocumentSearchSyncService {

    private final DocumentIndexingService documentIndexingService;

    public DocumentSearchSyncService(ObjectProvider<DocumentIndexingService> documentIndexingServiceProvider) {
        this.documentIndexingService = documentIndexingServiceProvider.getIfAvailable();
    }

    public void scheduleReindex(Long documentId) {
        if (documentId == null || documentIndexingService == null) {
            return;
        }
        runAfterCommit(() -> documentIndexingService.updateIndexById(documentId));
    }

    public void scheduleDelete(Long documentId) {
        if (documentId == null || documentIndexingService == null) {
            return;
        }
        runAfterCommit(() -> documentIndexingService.deleteFromIndex(documentId));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }

        task.run();
    }
}
