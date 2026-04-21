package com.studydocs.manager.controller.document;

import com.studydocs.manager.dto.document.DocumentSearchRequest;
import com.studydocs.manager.dto.document.DocumentSearchResponse;
import com.studydocs.manager.dto.document.DocumentSearchResult;
import com.studydocs.manager.search.DocumentIndexingService;
import com.studydocs.manager.search.DocumentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Document Search", description = "Elasticsearch-powered document search APIs")
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnProperty(name = "search.indexing.enabled", havingValue = "true", matchIfMissing = false)
public class DocumentSearchController {

    private final DocumentSearchService searchService;
    private final DocumentIndexingService indexingService;

    public DocumentSearchController(DocumentSearchService searchService,
            DocumentIndexingService indexingService) {
        this.searchService = searchService;
        this.indexingService = indexingService;
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Search public documents", description = "Full-text search over public published documents")
    public ResponseEntity<DocumentSearchResponse> searchDocuments(@RequestBody DocumentSearchRequest request) {
        return ResponseEntity.ok(searchService.searchDocuments(request));
    }

    @GetMapping("/autocomplete")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Autocomplete suggestions", description = "Autocomplete over public published document titles")
    public ResponseEntity<Map<String, Object>> autocomplete(@RequestParam("q") String query) {
        List<String> suggestions = searchService.autocomplete(query);
        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("suggestions", suggestions);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/similar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Find similar public documents", description = "Find public published documents similar to the given document")
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

    @PostMapping("/admin/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk re-index public documents", description = "Re-index all public published documents into Elasticsearch")
    public ResponseEntity<Map<String, Object>> bulkReindex() {
        int indexed = indexingService.bulkIndexAllDocuments();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk re-indexing completed");
        response.put("documentsIndexed", indexed);
        response.put("indexedDocuments", indexingService.countIndexedDocuments());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/reindex/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-index single document", description = "Re-index a specific document by ID")
    public ResponseEntity<Map<String, Object>> reindexDocument(@PathVariable Long id) {
        boolean indexed = indexingService.reindexDocumentNow(id);
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", id);
        response.put("indexed", indexed);
        response.put("indexedDocuments", indexingService.countIndexedDocuments());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get search index statistics", description = "Get simple statistics about the document search index")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("indexedDocuments", indexingService.countIndexedDocuments());
        return ResponseEntity.ok(stats);
    }
}
