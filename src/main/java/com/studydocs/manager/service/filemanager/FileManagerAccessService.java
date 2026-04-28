package com.studydocs.manager.service.filemanager;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.ForbiddenException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.UnauthorizedException;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class FileManagerAccessService {

    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public FileManagerAccessService(
            FolderRepository folderRepository,
            UserRepository userRepository,
            SecurityUtils securityUtils) {
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    public User requireActor() {
        Long currentUserId = requireCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));
    }

    public Long requireCurrentUserId() {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated", "USER_NOT_AUTHENTICATED", null);
        }
        return currentUserId;
    }

    public boolean isAdmin(User actor) {
        return actor.getRole() != null && "ADMIN".equals(actor.getRole().getName());
    }

    /**
     * Resolves the target folder for a transfer operation.
     *
     * <p>When {@code targetFolderId} is {@code null}, the method returns {@code null} to represent
     * the user's <b>root directory</b> (i.e. items with {@code parentId = null}).
     * This is the standard convention for all move, copy, and paste operations:
     * omitting the target folder ID means "transfer to root".
     *
     * @param targetFolderId the target folder ID, or {@code null} for root
     * @param userId         the current user's ID for ownership validation
     * @return the resolved {@link Folder}, or {@code null} if targeting root
     */
    public Folder resolveTargetFolder(Long targetFolderId, Long userId) {
        if (targetFolderId == null) {
            return null; // root directory — items will have parentId = null
        }

        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(targetFolderId)
                .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "targetFolderId"));
        if (!folder.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Folder does not belong to current user", "FOLDER_ACCESS_DENIED", "targetFolderId");
        }
        return folder;
    }

    public void validateCurrentFolderContext(Long currentFolderId, User actor) {
        if (currentFolderId == null) {
            return;
        }

        Folder currentFolder = folderRepository.findByIdAndDeletedAtIsNull(currentFolderId)
                .orElseThrow(() -> new NotFoundException(
                        "Current folder not found",
                        "CURRENT_FOLDER_NOT_FOUND",
                        "currentFolderId"));

        if (!currentFolder.getUser().getId().equals(actor.getId()) && !isAdmin(actor)) {
            throw new ForbiddenException(
                    "You don't have permission to access the current folder",
                    "CURRENT_FOLDER_ACCESS_DENIED",
                    "currentFolderId");
        }
    }

    public Folder findActiveFolderForUser(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(folderId)
                .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "id"));
        validateFolderOwnership(folder, userId);
        return folder;
    }

    public Folder findFolderForUser(Long folderId, Long userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "id"));
        validateFolderOwnership(folder, userId);
        return folder;
    }

    public void validateFolderOwnership(Folder folder, Long userId) {
        if (!folder.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Folder does not belong to current user", "FOLDER_ACCESS_DENIED", "id");
        }
    }

    public void validateDocumentOwnership(Document document, Long currentUserId, String action) {
        if (document.getUser().getId().equals(currentUserId)) {
            return;
        }

        User actor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));
        if (!isAdmin(actor)) {
            throw new ForbiddenException(
                    "You don't have permission to " + action + " this document",
                    "DOCUMENT_" + action.toUpperCase() + "_DENIED",
                    "id");
        }
    }
}
