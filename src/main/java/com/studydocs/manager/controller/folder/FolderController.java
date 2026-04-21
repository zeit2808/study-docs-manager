package com.studydocs.manager.controller.folder;

import com.studydocs.manager.application.folder.FolderApplicationService;
import com.studydocs.manager.dto.common.SuccessResponse;
import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderTrashResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@Tag(name = "Folder Management", description = "APIs for folder management: create, read, update, delete folders. Folders can be nested to form a hierarchy.")
public class FolderController {

    private static final Logger log = LoggerFactory.getLogger(FolderController.class);

    private final FolderApplicationService folderApplicationService;

    public FolderController(FolderApplicationService folderApplicationService) {
        this.folderApplicationService = folderApplicationService;
    }

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody FolderCreateRequest request
    ) {
        log.info("Create folder: name={}, parentId={}", request.getName(), request.getParentId());

        FolderResponse response = folderApplicationService.createFolder(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getFolders(
            @RequestParam(required = false) Long parentId
    ) {
        log.info("Get folders with parentId={}", parentId);

        List<FolderResponse> response = folderApplicationService.getFolders(parentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(
            @PathVariable Long id
    ) {
        log.info("Get folder id={}", id);

        FolderResponse response = folderApplicationService.getFolderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FolderTrashResponse>> getTrashFolders() {
        log.info("Get folder trash");

        List<FolderTrashResponse> response = folderApplicationService.getTrashFolders();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody FolderUpdateRequest request
    ) {
        log.info("Update folder id={}", id);

        FolderResponse response = folderApplicationService.updateFolder(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<FolderDeleteResult> deleteFolder(
            @PathVariable Long id
    ) {
        log.warn("Delete folder id={}", id);

        FolderDeleteResult result = folderApplicationService.deleteFolder(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<SuccessResponse> restoreFolder(
            @PathVariable Long id
    ) {
        log.info("Restore folder id={}", id);

        folderApplicationService.restoreFolder(id);
        return ResponseEntity.ok(new SuccessResponse(true, "Folder restored successfully."));
    }
}
