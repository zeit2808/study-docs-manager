package com.studydocs.manager.controller;

import com.studydocs.manager.dto.DocumentCreateRequest;
import com.studydocs.manager.dto.DocumentResponse;
import com.studydocs.manager.dto.DocumentUpdateRequest;
import com.studydocs.manager.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "APIs for managing documents")
@SecurityRequirement(name = "bearerAuth")

public class DocumentController {
    @Autowired
    private DocumentService documentService;

    @PostMapping
    @Operation(summary = "Create document", description = "Create a new document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentCreateRequest request){
        try{
            DocumentResponse response = documentService.createDocument(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Get document details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        try {
            DocumentResponse response = documentService.getDocumentById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document", description = "Update document information")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUpdateRequest request) {
        try {
            DocumentResponse response = documentService.updateDocument(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Soft delete a document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore document", description = "Restore a deleted document")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<DocumentResponse> restoreDocument(@PathVariable Long id) {
        try {
            DocumentResponse response = documentService.restoreDocument(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
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

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documents = documentService.getMyDocuments(status, folderId, pageable);
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

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<DocumentResponse> documents = documentService.getPublicDocuments(status, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents", description = "Search documents by keyword")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DocumentResponse> documents = documentService.searchDocuments(q, pageable);
        return ResponseEntity.ok(documents);
    }


}
