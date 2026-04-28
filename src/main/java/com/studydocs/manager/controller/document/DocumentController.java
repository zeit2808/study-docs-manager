package com.studydocs.manager.controller.document;

import com.studydocs.manager.application.document.DocumentApplicationService;
import com.studydocs.manager.dto.common.SuccessResponse;
import com.studydocs.manager.dto.document.DocumentCreateRequest;
import com.studydocs.manager.dto.document.DocumentCreateResponse;
import com.studydocs.manager.dto.document.DocumentResponse;
import com.studydocs.manager.dto.document.DocumentUpdateRequest;
import com.studydocs.manager.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "APIs for managing documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {
    private final DocumentApplicationService documentApplicationService;

    public DocumentController(DocumentApplicationService documentApplicationService) {
        this.documentApplicationService = documentApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create document", description = "Create a new document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentCreateResponse> createDocument(@Valid @RequestBody DocumentCreateRequest request) {
        DocumentCreateResponse response = documentApplicationService.createDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Get document details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        DocumentResponse response = documentApplicationService.getDocumentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document", description = "Update document information")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request) {
        DocumentResponse response = documentApplicationService.updateDocument(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Soft delete a document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentApplicationService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore document", description = "Restore a deleted document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SuccessResponse> restoreDocument(@PathVariable Long id) {
        documentApplicationService.restoreDocument(id);
        return ResponseEntity.ok(new SuccessResponse(true, "Document restored successfully."));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my documents", description = "Get list of current user's documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> getMyDocuments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documents = documentApplicationService.getMyDocuments(status, folderId, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public documents", description = "Get list of public documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> getPublicDocuments(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documents = documentApplicationService.getPublicDocuments(status, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document file", description = "Download the file associated with a document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> downloadDocument(@PathVariable Long id) {
        DocumentResponse document = documentApplicationService.getDocumentById(id);

        if (document.getObjectName() == null || document.getObjectName().isEmpty()) {
            throw new NotFoundException("Document file not found", "DOCUMENT_FILE_NOT_FOUND", "objectName");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/api/files/download?objectName=" + document.getObjectName())
                .build();
    }

    @GetMapping("/trash")
    @Operation(summary = "Get my trash", description = "Get paginated list of soft-deleted documents (Trash)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> getMyTrash(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "deletedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(documentApplicationService.getMyTrash(pageable));
    }

}
