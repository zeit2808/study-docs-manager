package com.studydocs.manager.service.folder;

import com.studydocs.manager.dto.folder.FolderCreateRequest;
import com.studydocs.manager.dto.folder.FolderDeleteResult;
import com.studydocs.manager.dto.folder.FolderResponse;
import com.studydocs.manager.dto.folder.FolderUpdateRequest;
import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.DocumentRepository;
import com.studydocs.manager.repository.FolderRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderService {
    @Autowired
    private FolderRepository folderRepository;
    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request) {
        Long userId = requireCurrentUser();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        // Validate duplicate name trong cùng parent
        validateUniqueName(userId, request.getName(), request.getParentId());
        Folder folder = new Folder();
        folder.setUser(user);
        folder.setName(request.getName().trim());
        folder.setColor(request.getColor());
        folder.setIcon(request.getIcon());
        folder.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        if (request.getParentId() != null) {
            Folder parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            if (!parent.getUser().getId().equals(userId)) {
                throw new RuntimeException("Parent folder does not belong to current user");
            }
            folder.setParent(parent);
        }
        return toResponse(folderRepository.save(folder));
    }

    public List<FolderResponse> getMyFolders(Long parentId) {
        Long userId = requireCurrentUser();
        List<Folder> folders = (parentId == null) ? folderRepository.findByUserIdAndParentIdIsNull(userId)
                : folderRepository.findByUserIdAndParentId(userId, parentId);
        return folders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FolderResponse getFolderById(Long id) {
        Long userId = requireCurrentUser();
        Folder folder = findFolderForUser(id, userId);
        return toResponse(folder);
    }

    @Transactional
    public FolderResponse updateFolder(Long id, FolderUpdateRequest request) {
        Long userId = requireCurrentUser();
        Folder folder = findFolderForUser(id, userId);
        if (request.getName() != null && !request.getName().equals(folder.getName())) {
            validateUniqueName(userId, request.getName(), folder.getParent() != null
                    ? folder.getParent().getId()
                    : null);
            folder.setName(request.getName().trim());
        }
        if (request.getColor() != null)
            folder.setColor(request.getColor());
        if (request.getIcon() != null)
            folder.setIcon(request.getIcon());
        if (request.getSortOrder() != null)
            folder.setSortOrder(request.getSortOrder());
        // Di chuyển folder sang parent khác
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("A folder cannot be its own parent");
            }
            Folder newParent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            if (!newParent.getUser().getId().equals(userId)) {
                throw new RuntimeException("Parent folder does not belong to current user");
            }
            folder.setParent(newParent);
        }
        return toResponse(folderRepository.save(folder));
    }

    @Transactional
    public FolderDeleteResult deleteFolder(Long id, boolean cascade) {
        Long userId = requireCurrentUser();
        Folder folder = findFolderForUser(id, userId);
        int affectedDocuments = 0;
        if (cascade) {
            affectedDocuments = deleteFolderCascade(folder, userId);
        } else {
            affectedDocuments = unlinkDocuments(folder, userId);
            unlinkChildFolders(folder);
        }
        // Clear collections before delete to prevent stale cascade
        folder.getChildren().clear();
        folder.getDocuments().clear();
        folderRepository.delete(folder);
        return new FolderDeleteResult(id, cascade, affectedDocuments);
    }


    private int deleteFolderCascade(Folder folder, Long userId) {
        int count = 0;
        // Xử lý sub-folders trước (recursive)
        List<Folder> children = new ArrayList<>(folder.getChildren());
        for (Folder child : children) {
            count += deleteFolderCascade(child, userId);
            child.getChildren().clear();
            child.getDocuments().clear();
            folderRepository.delete(child);
        }
        // Soft-delete documents
        List<Document> docs = documentRepository.findAllByFolderIdAndDeletedAtIsNull(folder.getId());
        User currentUser = userRepository.getReferenceById(userId);
        for (Document doc : docs) {
            doc.setDeletedAt(LocalDateTime.now());
            doc.setDeletedBy(currentUser);
            doc.setStatus(Document.DocumentStatus.DELETED);
            doc.setFolder(null);
            count++;
        }
        documentRepository.saveAll(docs);
        folder.setDocumentCount(0);
        return count;
    }


    /** Unlink documents — set folder = null, KHÔNG xóa */
    private int unlinkDocuments(Folder folder, Long userId) {
        List<Document> docs = documentRepository.findAllByFolderIdAndDeletedAtIsNull(folder.getId());
        for (Document doc : docs) {
            doc.setFolder(null);
        }
        documentRepository.saveAll(docs);
        folder.setDocumentCount(0);
        return docs.size();
    }



    /** Unlink sub-folders — set parent = null (trở thành root) */
    private void unlinkChildFolders(Folder folder) {
        List<Folder> children = new ArrayList<>(folder.getChildren());
        for (Folder child : children) {
            child.setParent(null);
        }
        folderRepository.saveAll(children);
    }


    private Folder findFolderForUser(Long folderId, Long userId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        if (!folder.getUser().getId().equals(userId)) {
            throw new RuntimeException("Folder does not belong to current user");
        }
        return folder;
    }

    private Long requireCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }
        return userId;
    }

    private void validateUniqueName(Long userId, String name, Long parentId) {
        boolean exists = (parentId == null)
                ? folderRepository.existsByUserIdAndNameAndParentIdIsNull(userId, name.trim())
                : folderRepository.existsByUserIdAndNameAndParentId(userId, name.trim(), parentId);
        if (exists) {
            throw new RuntimeException("A folder with this name already exists in this location");
        }
    }

    private FolderResponse toResponse(Folder folder) {
        FolderResponse r = new FolderResponse();
        r.setId(folder.getId());
        r.setName(folder.getName());
        r.setParentId(folder.getParent() != null ? folder.getParent().getId() : null);
        r.setColor(folder.getColor());
        r.setIcon(folder.getIcon());
        r.setSortOrder(folder.getSortOrder());
        r.setDocumentCount(folder.getDocumentCount());
        r.setCreatedAt(folder.getCreatedAt());
        r.setUpdatedAt(folder.getUpdatedAt());
        return r;
    }
}
