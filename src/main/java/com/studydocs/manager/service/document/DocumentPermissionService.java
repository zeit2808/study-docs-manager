package com.studydocs.manager.service.document;

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

/**
 * Centralises authentication and authorization checks for documents.
 * Extracted from DocumentService to satisfy Single Responsibility Principle.
 */
@Service
public class DocumentPermissionService {

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    public DocumentPermissionService(
            SecurityUtils securityUtils,
            UserRepository userRepository,
            FolderRepository folderRepository) {
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.folderRepository = folderRepository;
    }

    /**
     * Returns the current authenticated user's ID.
     * Throws UnauthorizedException if no user is authenticated.
     */
    public Long requireCurrentUserId() {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated", "USER_NOT_AUTHENTICATED", null);
        }
        return currentUserId;
    }

    /**
     * Returns the current user's ID, or null if not authenticated.
     */
    public Long getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }

    /**
     * Validates that the current user owns the document, or is an ADMIN.
     */
    public void validateDocumentOwnership(Document document, Long currentUserId, String action) {
        if (document.getUser().getId().equals(currentUserId)) {
            return;
        }

        User actor = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", null));
        boolean isAdmin = actor.getRole() != null && "ADMIN".equals(actor.getRole().getName());
        if (!isAdmin) {
            throw new ForbiddenException(
                    "You don't have permission to " + action + " this document",
                    "DOCUMENT_" + action.toUpperCase() + "_DENIED",
                    "id");
        }
    }

    /**
     * Validates that a folder belongs to the given user.
     * Returns the resolved Folder entity.
     */
    public Folder validateFolderOwnership(Long folderId, Long currentUserId) {
        Folder folder = folderRepository.findByIdAndDeletedAtIsNull(folderId)
                .orElseThrow(() -> new NotFoundException("Folder not found", "FOLDER_NOT_FOUND", "folderId"));
        if (!folder.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(
                    "Folder does not belong to current user", "FOLDER_ACCESS_DENIED", "folderId");
        }
        return folder;
    }
}
