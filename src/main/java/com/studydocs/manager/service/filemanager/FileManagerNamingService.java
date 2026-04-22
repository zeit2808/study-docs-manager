package com.studydocs.manager.service.filemanager;

import org.springframework.stereotype.Service;

@Service
public class FileManagerNamingService {

    private final FileManagerNamePolicy fileManagerNamePolicy;
    private final FileManagerNamespaceService fileManagerNamespaceService;

    public FileManagerNamingService(
            FileManagerNamePolicy fileManagerNamePolicy,
            FileManagerNamespaceService fileManagerNamespaceService) {
        this.fileManagerNamePolicy = fileManagerNamePolicy;
        this.fileManagerNamespaceService = fileManagerNamespaceService;
    }

    public void validateDocumentNameAvailable(Long userId, Long folderId, String candidateName, Long ignoredDocumentId) {
        fileManagerNamespaceService.ensureDocumentNameAvailable(userId, folderId, candidateName, ignoredDocumentId);
    }

    public String resolveDocumentDisplayName(String requestedDisplayName, String fileName, String title) {
        return fileManagerNamePolicy.requireDocumentName(requestedDisplayName, fileName, title);
    }
}
