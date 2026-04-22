package com.studydocs.manager.application.document;

import com.studydocs.manager.application.document.usecase.CreateDocumentUseCase;
import com.studydocs.manager.application.document.usecase.DocumentQueryUseCase;
import com.studydocs.manager.application.document.usecase.EmptyTrashDocumentsUseCase;
import com.studydocs.manager.application.document.usecase.PermanentDeleteDocumentUseCase;
import com.studydocs.manager.application.document.usecase.RestoreDocumentCommandUseCase;
import com.studydocs.manager.application.document.usecase.SoftDeleteDocumentUseCase;
import com.studydocs.manager.application.document.usecase.UpdateDocumentUseCase;
import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.dto.document.DocumentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DocumentApplicationService {

    private final CreateDocumentUseCase createDocumentUseCase;
    private final UpdateDocumentUseCase updateDocumentUseCase;
    private final SoftDeleteDocumentUseCase softDeleteDocumentUseCase;
    private final RestoreDocumentCommandUseCase restoreDocumentUseCase;
    private final PermanentDeleteDocumentUseCase permanentDeleteDocumentUseCase;
    private final EmptyTrashDocumentsUseCase emptyTrashDocumentsUseCase;
    private final DocumentQueryUseCase documentQueryUseCase;

    public DocumentApplicationService(
            CreateDocumentUseCase createDocumentUseCase,
            UpdateDocumentUseCase updateDocumentUseCase,
            SoftDeleteDocumentUseCase softDeleteDocumentUseCase,
            RestoreDocumentCommandUseCase restoreDocumentUseCase,
            PermanentDeleteDocumentUseCase permanentDeleteDocumentUseCase,
            EmptyTrashDocumentsUseCase emptyTrashDocumentsUseCase,
            DocumentQueryUseCase documentQueryUseCase) {
        this.createDocumentUseCase = createDocumentUseCase;
        this.updateDocumentUseCase = updateDocumentUseCase;
        this.softDeleteDocumentUseCase = softDeleteDocumentUseCase;
        this.restoreDocumentUseCase = restoreDocumentUseCase;
        this.permanentDeleteDocumentUseCase = permanentDeleteDocumentUseCase;
        this.emptyTrashDocumentsUseCase = emptyTrashDocumentsUseCase;
        this.documentQueryUseCase = documentQueryUseCase;
    }

    public DocumentResponse createDocument(DocumentCreateRequest request) {
        return createDocumentUseCase.execute(request);
    }

    public DocumentResponse getDocumentById(Long id) {
        return documentQueryUseCase.getDocumentById(id);
    }

    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request) {
        return updateDocumentUseCase.execute(id, request);
    }

    public void deleteDocument(Long id) {
        softDeleteDocumentUseCase.execute(id);
    }

    public DocumentResponse restoreDocument(Long id) {
        return restoreDocumentUseCase.execute(id);
    }

    public Page<DocumentResponse> getMyDocuments(String status, Long folderId, Pageable pageable) {
        return documentQueryUseCase.getMyDocuments(status, folderId, pageable);
    }

    public Page<DocumentResponse> getPublicDocuments(String status, Pageable pageable) {
        return documentQueryUseCase.getPublicDocuments(status, pageable);
    }

    public Page<DocumentResponse> getMyTrash(Pageable pageable) {
        return documentQueryUseCase.getMyTrash(pageable);
    }

    public void permanentDeleteDocument(Long id) {
        permanentDeleteDocumentUseCase.execute(id);
    }

    public void emptyTrash() {
        emptyTrashDocumentsUseCase.execute();
    }
}
