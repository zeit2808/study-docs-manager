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

        validateTargetFolder(targetFolder, selections);
        return operation == ClipboardOperation.MOVE
                ? moveItemsUseCase.execute(selections, targetFolder, actor)
                : copyItemsUseCase.execute(selections, targetFolder, actor);
    }

    private void validateTargetFolder(Folder targetFolder, List<FileManagerSelection> selections) {
        if (targetFolder == null) {
            return;
        }

        for (FileManagerSelection selection : selections) {
            if (selection.type() == FileManagerItemType.FOLDER
                    && fileManagerTreeService.isDescendantOrSelf(targetFolder, selection.folder())) {
                throw new BadRequestException(
                        "A folder cannot be moved or copied into itself or one of its descendants",
                        "INVALID_TARGET_FOLDER",
                        "targetFolderId");
            }
        }
    }
}
