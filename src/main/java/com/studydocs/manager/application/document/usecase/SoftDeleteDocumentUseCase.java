package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.application.filemanager.FileManagerApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SoftDeleteDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SoftDeleteDocumentUseCase.class);

    private final FileManagerApplicationService fileManagerApplicationService;

    public SoftDeleteDocumentUseCase(FileManagerApplicationService fileManagerApplicationService) {
        this.fileManagerApplicationService = fileManagerApplicationService;
    }

    public void execute(Long id) {
        fileManagerApplicationService.deleteDocument(id);
        logger.info("Document deleted (soft) via file-manager command flow - id: {}", id);
    }
}
