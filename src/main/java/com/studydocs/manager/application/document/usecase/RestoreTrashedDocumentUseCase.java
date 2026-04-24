package com.studydocs.manager.application.document.usecase;

import com.studydocs.manager.application.filemanager.FileManagerApplicationService;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.service.filemanager.FileManagerResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RestoreTrashedDocumentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RestoreTrashedDocumentUseCase.class);

    private final FileManagerApplicationService fileManagerApplicationService;
    private final FileManagerResponseMapper fileManagerResponseMapper;

    public RestoreTrashedDocumentUseCase(
            FileManagerApplicationService fileManagerApplicationService,
            FileManagerResponseMapper fileManagerResponseMapper) {
        this.fileManagerApplicationService = fileManagerApplicationService;
        this.fileManagerResponseMapper = fileManagerResponseMapper;
    }

    public DocumentResponse execute(Long id) {
        Document restored = fileManagerApplicationService.restoreDocument(id);
        logger.info("Document restored via file-manager command flow - id: {}", id);
        return fileManagerResponseMapper.toDocumentResponse(restored);
    }
}
