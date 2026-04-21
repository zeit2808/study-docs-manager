package com.studydocs.manager.application.document;

import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.dto.document.DocumentUpdateRequest;
import com.studydocs.manager.service.document.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DocumentApplicationService {

    private final DocumentService documentService;

    public DocumentApplicationService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocumentResponse createDocument(DocumentCreateRequest request) {
        return documentService.createDocument(request);
    }

    public DocumentResponse getDocumentById(Long id) {
        return documentService.getDocumentById(id);
    }

    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request) {
        return documentService.updateDocument(id, request);
    }

    public void deleteDocument(Long id) {
        documentService.deleteDocument(id);
    }

    public DocumentResponse restoreDocument(Long id) {
        return documentService.restoreDocument(id);
    }

    public Page<DocumentResponse> getMyDocuments(String status, Long folderId, Pageable pageable) {
        return documentService.getMyDocuments(status, folderId, pageable);
    }

    public Page<DocumentResponse> getPublicDocuments(String status, Pageable pageable) {
        return documentService.getPublicDocuments(status, pageable);
    }

    public Page<DocumentResponse> getMyTrash(Pageable pageable) {
        return documentService.getMyTrash(pageable);
    }

    public void permanentDeleteDocument(Long id) {
        documentService.permanentDeleteDocument(id);
    }

    public void emptyTrash() {
        documentService.emptyTrash();
    }
}
