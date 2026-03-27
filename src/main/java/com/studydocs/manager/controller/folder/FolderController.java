package com.studydocs.manager.controller.folder;
import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.service.folder.FolderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "Folder Management", description = "APIs for folder management: create, read, update, delete folders. Folders can be nested to form a hierarchy.")
public class FolderController {

    private static final Logger log = LoggerFactory.getLogger(FolderController.class);

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * Create folder
     */
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody FolderCreateRequest request
    ) {
        log.info("Create folder: name={}, parentId={}", request.getName(), request.getParentId());

        FolderResponse response = folderService.createFolder(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get folders (root hoặc theo parentId)
     * GET /api/v1/folders?parentId=1
     */
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getFolders(
            @RequestParam(required = false) Long parentId
    ) {
        log.info("Get folders with parentId={}", parentId);

        List<FolderResponse> response = folderService.getMyFolders(parentId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get folder by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(
            @PathVariable Long id
    ) {
        log.info("Get folder id={}", id);

        FolderResponse response = folderService.getFolderById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Update folder
     */
    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody FolderUpdateRequest request
    ) {
        log.info("Update folder id={}", id);

        FolderResponse response = folderService.updateFolder(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete folder
     * cascade=true: xóa cả cây + documents
     * cascade=false: chỉ unlink
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<FolderDeleteResult> deleteFolder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cascade
    ) {
        log.warn("Delete folder id={}, cascade={}", id, cascade);

        FolderDeleteResult result = folderService.deleteFolder(id, cascade);

        return ResponseEntity.ok(result);
    }
}