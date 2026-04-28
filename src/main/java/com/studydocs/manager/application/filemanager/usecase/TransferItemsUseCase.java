package com.studydocs.manager.application.filemanager.usecase;

import com.studydocs.manager.dto.filemanager.FileManagerPasteResponse;
import com.studydocs.manager.dto.filemanager.FileManagerTransferRequest;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.ClipboardOperation;
import com.studydocs.manager.enums.FileManagerItemType;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.service.filemanager.FileManagerAccessService;
import com.studydocs.manager.service.filemanager.FileManagerSelection;
import com.studydocs.manager.service.filemanager.FileManagerSelectionResolver;
import com.studydocs.manager.service.filemanager.FileManagerTreeService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransferItemsUseCase {

    private final FileManagerSelectionResolver fileManagerSelectionResolver;
    private final FileManagerTreeService fileManagerTreeService;
    private final FileManagerAccessService fileManagerAccessService;
    private final MoveItemsUseCase moveItemsUseCase;
    private final CopyItemsUseCase copyItemsUseCase;

    public TransferItemsUseCase(
            FileManagerSelectionResolver fileManagerSelectionResolver,
            FileManagerTreeService fileManagerTreeService,
            FileManagerAccessService fileManagerAccessService,
            MoveItemsUseCase moveItemsUseCase,
            CopyItemsUseCase copyItemsUseCase) {
        this.fileManagerSelectionResolver = fileManagerSelectionResolver;
        this.fileManagerTreeService = fileManagerTreeService;
        this.fileManagerAccessService = fileManagerAccessService;
        this.moveItemsUseCase = moveItemsUseCase;
        this.copyItemsUseCase = copyItemsUseCase;
    }

    @Transactional
    public FileManagerPasteResponse copy(FileManagerTransferRequest request) {
        return transfer(request, ClipboardOperation.COPY);
    }

    @Transactional
    public FileManagerPasteResponse move(FileManagerTransferRequest request) {
        return transfer(request, ClipboardOperation.MOVE);
    }

    private FileManagerPasteResponse transfer(FileManagerTransferRequest request, ClipboardOperation operation) {
        User actor = fileManagerAccessService.requireActor();
        Folder targetFolder = fileManagerAccessService.resolveTargetFolder(request.getTargetFolderId(), actor.getId());
        List<FileManagerSelection> selections = fileManagerSelectionResolver
                .canonicalizeOwnedSelection(request.getItems(), actor.getId());
        if (selections.isEmpty()) {
            throw new BadRequestException("No actionable items selected", "EMPTY_SELECTION", "items");
        }

        validateTargetFolder(targetFolder, selections, operation);
        return operation == ClipboardOperation.MOVE
                ? moveItemsUseCase.execute(selections, targetFolder, actor)
                : copyItemsUseCase.execute(selections, targetFolder, actor);
    }

    /**
     * Validates that the target folder is a legal destination for the given selections.
     *
     * <p><b>Tree-cycle prevention (MOVE):</b> Moving folder A into any of its own descendants
     * would create a circular parent reference (A → … → descendant → A), violating the acyclic
     * tree invariant. Every recursive traversal ({@code collectActiveFolderTree},
     * {@code folderDepth}, breadcrumb rendering, etc.) would enter an infinite loop.
     *
     * <p><b>Infinite-clone prevention (COPY):</b> Copying folder A into any of its own
     * descendants would require the clone operation to recursively clone A's subtree — which
     * includes the very descendant that is the target — producing a logically unbounded copy.
     *
     * <p>Both cases are guarded by {@link FileManagerTreeService#isDescendantOrSelf}.
     * The error codes are split by operation and by specificity (self vs. descendant) so the
     * frontend can display precise, localised messages.
     *
     * <p><b>targetFolder == null (root):</b> {@code null} represents the user's root directory
     * (parentId&nbsp;=&nbsp;null). Root is always a valid destination — no structural conflict
     * is possible — so validation is skipped entirely.
     *
     * @param targetFolder the resolved target folder, or {@code null} for root
     * @param selections   the items being transferred
     * @param operation    MOVE or COPY — determines which error codes to use
     */
    private void validateTargetFolder(
            Folder targetFolder,
            List<FileManagerSelection> selections,
            ClipboardOperation operation) {

        // null = root directory (parentId = null) — always a valid target for any operation
        if (targetFolder == null) {
            return;
        }

        for (FileManagerSelection selection : selections) {
            if (selection.type() != FileManagerItemType.FOLDER) {
                continue;
            }

            Folder sourceFolder = selection.folder();
            if (!fileManagerTreeService.isDescendantOrSelf(targetFolder, sourceFolder)) {
                continue;
            }

            // Distinguish "into itself" vs "into a descendant" for clearer frontend messaging
            boolean isSelf = fileManagerTreeService.sameFolder(targetFolder, sourceFolder);

            if (operation == ClipboardOperation.MOVE) {
                throw new BadRequestException(
                        isSelf ? "A folder cannot be moved into itself"
                               : "A folder cannot be moved into one of its own subfolders",
                        isSelf ? "MOVE_INTO_SELF" : "MOVE_INTO_DESCENDANT",
                        "targetFolderId");
            }

            throw new BadRequestException(
                    isSelf ? "A folder cannot be copied into itself"
                           : "A folder cannot be copied into one of its own subfolders",
                    isSelf ? "COPY_INTO_SELF" : "COPY_INTO_DESCENDANT",
                    "targetFolderId");
        }
    }
}
