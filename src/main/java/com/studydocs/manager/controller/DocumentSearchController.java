package com.studydocs.manager.controller;

import com.studydocs.manager.dto.DocumentSearchRequest;
import com.studydocs.manager.dto.DocumentSearchResponse;
import com.studydocs.manager.dto.DocumentSearchResult;
import com.studydocs.manager.search.DocumentIndexingService;
import com.studydocs.manager.search.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho document search operations
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "Document Search", description = "Elasticsearch-powered document search APIs")
public class DocumentSearchController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSearchController.class);

    @Autowired
    private DocumentSearchService searchService;

    @Autowired
    private DocumentIndexingService indexingService;

    /**
     * Advanced document search với filters, sorting và highlighting
     * 
     * POST /api/search/documents
     */
    @PostMapping("/documents")
    @Operation(summary = "Search documents", description = "Advanced search với multi-field text search, filters, sorting và highlighting")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(
            @RequestBody DocumentSearchRequest request) {

        logger.info("Search request: query='{}', page={}, size={}",
                request.getQuery(), request.getPage(), request.getSize());

        DocumentSearchResponse response = searchService.searchDocuments(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Autocomplete suggestions
     * 
     * GET /api/search/autocomplete?q=keyword
     */
    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete suggestions", description = "Get autocomplete suggestions for document titles")
    public ResponseEntity<Map<String, Object>> autocomplete(
            @RequestParam("q") String query) {

        List<String> suggestions = searchService.autocomplete(query);

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("suggestions", suggestions);

        return ResponseEntity.ok(response);
    }

    /**
     * Find similar documents (More Like This)
     * 
     * GET /api/search/similar/{id}
     */
    @GetMapping("/similar/{id}")
    @Operation(summary = "Find similar documents", description = "Find documents similar to the given document using More Like This query")
    public ResponseEntity<Map<String, Object>> findSimilar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {

        List<DocumentSearchResult> similar = searchService.findSimilarDocuments(id, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("documentId", id);
        response.put("similar", similar);
        response.put("count", similar.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Re-index all documents (Admin only)
     * 
     * POST /api/admin/search/reindex
     */
    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Bulk re-index all documents", description = "Re-index all PUBLISHED documents into Elasticsearch (Admin only)")
    public ResponseEntity<Map<String, Object>> bulkReindex() {
        logger.info("Starting bulk re-indexing...");

        int indexed = indexingService.bulkIndexAllDocuments();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk re-indexing completed");
        response.put("documentsIndexed", indexed);

        logger.info("Bulk re-indexing completed: {} documents", indexed);
        return ResponseEntity.ok(response);
    }

    /**
     * Re-index single document (Admin only)
     * 
     * POST /api/admin/search/reindex/{id}
     */
    @PostMapping("/admin/reindex/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Re-index single document", description = "Re-index a specific document by ID (Admin only)")
    public ResponseEntity<Map<String, Object>> reindexDocument(@PathVariable Long id) {
        logger.info("Re-indexing document: {}", id);

        // This would need DocumentService to fetch the document
        // For now, just return acknowledgment
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Document re-index requested");
        response.put("documentId", id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get search statistics (Admin only)
     * 
     * GET /api/admin/search/stats
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get search index statistics", description = "Get statistics about the Elasticsearch index (Admin only)")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        // This would fetch stats from Elasticsearch
        // For now, return placeholder
        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Search statistics endpoint");

        return ResponseEntity.ok(stats);
    }
}
