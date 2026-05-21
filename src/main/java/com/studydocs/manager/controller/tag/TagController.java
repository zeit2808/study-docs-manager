package com.studydocs.manager.controller.tag;

import com.studydocs.manager.application.tag.TagApplicationService;
import com.studydocs.manager.dto.tag.TagCreateRequest;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.dto.tag.TagUpdateRequest;
import com.studydocs.manager.web.PageableSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "APIs for managing tags")
@SecurityRequirement(name = "bearerAuth")
public class TagController {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("id", "name", "slug", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("name"), Sort.Order.desc("id"));

    private final TagApplicationService tagApplicationService;

    public TagController(TagApplicationService tagApplicationService) {
        this.tagApplicationService = tagApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create tag", description = "Create a new tag")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagApplicationService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Get tag details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TagResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tagApplicationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List tags", description = "Get paginated list of active tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<TagResponse>> list(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable, ALLOWED_SORT_PROPERTIES, DEFAULT_SORT);
        return ResponseEntity.ok(tagApplicationService.list(keyword, safePageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Update a tag")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TagResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TagUpdateRequest request) {
        return ResponseEntity.ok(tagApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag", description = "Soft delete a tag")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
