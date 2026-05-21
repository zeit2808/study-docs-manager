package com.studydocs.manager.controller.subject;

import com.studydocs.manager.application.subject.SubjectApplicationService;
import com.studydocs.manager.dto.subject.SubjectCreateRequest;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.dto.subject.SubjectUpdateRequest;
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
@RequestMapping("/api/subjects")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Subjects", description = "APIs for managing subjects")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("id", "name", "slug", "sortOrder", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.desc("id"));

    private final SubjectApplicationService subjectApplicationService;

    public SubjectController(SubjectApplicationService subjectApplicationService) {
        this.subjectApplicationService = subjectApplicationService;
    }

    @PostMapping
    @Operation(summary = "Create subject", description = "Create a new subject")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectApplicationService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID", description = "Get subject details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectApplicationService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List subjects", description = "Get paginated list of active subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<SubjectResponse>> list(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable, ALLOWED_SORT_PROPERTIES, DEFAULT_SORT);
        return ResponseEntity.ok(subjectApplicationService.list(keyword, safePageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject", description = "Update a subject")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubjectResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubjectUpdateRequest request) {
        return ResponseEntity.ok(subjectApplicationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete subject", description = "Soft delete a subject")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectApplicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
