package com.studydocs.manager.service;

import com.studydocs.manager.dto.DocumentCreateRequest;
import com.studydocs.manager.dto.DocumentResponse;
import com.studydocs.manager.dto.DocumentUpdateRequest;
import com.studydocs.manager.entity.*;
import com.studydocs.manager.repository.*;
import com.studydocs.manager.security.SecurityUtils;
import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private DocumentSubjectRepository documentSubjectRepository;

    @Autowired
    private DocumentTagRepository documentTagRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired(required = false)
    private DocumentEventService documentEventService;

    @Transactional
    public DocumentResponse createDocument(DocumentCreateRequest request){
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null){
            throw new RuntimeException("User not authenticated");
        }
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Document document = new Document();
        document.setUser(user);
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setContent(request.getContent());
        document.setFileUrl(request.getFileUrl());
        document.setFileName(request.getFileName());
        document.setFileSize(request.getFileSize());
        document.setFileType(request.getFileType());
        document.setThumbnailUrl(request.getThumbnailUrl());
        document.setLanguage(request.getLanguage() != null ? request.getLanguage() : "vi");
        document.setStatus(Document.DocumentStatus.DRAFT);
        document.setVisibility(Document.DocumentVisibility.PRIVATE);
        document.setCreatedBy(user);

        if (request.getFolderId() != null){
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            if (!folder.getUser().getId().equals(currentUserId)){
                throw new RuntimeException("Folder does not belong to current user");
            }
            document.setFolder(folder);
        }

        Document saved = documentRepository.save(document);

        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()){
            assignSubjects(saved,request.getSubjectIds());
        }

        if (request.getTagNames() != null && !request.getTagNames().isEmpty()){
            assignTags(saved,request.getTagNames());
        }

        logEvent(saved, DocumentEvent.DocumentEventType.CREATED,"Document created");

        logger.info("Document created - id: {},title: {}, userId: {}, saved.getId(), saved.getTitle(), currentUserId");
        return  convertToResponse(saved);
    }

    @Transactional
    public DocumentResponse updateDocument(Long id, DocumentUpdateRequest request){
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null){
            throw  new RuntimeException("User not authenticated");
        }

        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUser().getId().equals(currentUserId)){
            boolean isAdmin = document.getUser().getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ADMIN"));
            if (!isAdmin){
                throw new RuntimeException("You don't have permission to update this document");
            }
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            document.setDescription(request.getDescription());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        if (request.getFileUrl() != null) {
            document.setFileUrl(request.getFileUrl());
        }
        if (request.getFileName() != null) {
            document.setFileName(request.getFileName());
        }
        if (request.getFileSize() != null) {
            document.setFileSize(request.getFileSize());
        }
        if (request.getFileType() != null) {
            document.setFileType(request.getFileType());
        }
        if (request.getThumbnailUrl() != null) {
            document.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getLanguage() != null) {
            document.setLanguage(request.getLanguage());
        }
        if (request.getStatus() != null){
            try{
                document.setStatus(Document.DocumentStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e){
                throw  new RuntimeException(("Invalid status: " + request.getStatus()));
            }
        }
        if (request.getVisibility() != null){
            try {
                document.setVisibility(Document.DocumentVisibility.valueOf(request.getVisibility()));
            } catch (IllegalArgumentException e){
                throw new RuntimeException("Invalid visibility: " + request.getVisibility());
            }
        }
        if (request.getIsFeatured() != null){
            document.setIsFeatured((request.getIsFeatured()));
        }

        if (request.getFolderId() != null){
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            if (!folder.getUser().getId().equals(currentUserId)){
                throw  new RuntimeException("Folder does not belong to current user");
            }
            document.setFolder(folder);
        }
        else if (request.getFolderId() == null && document.getFolder() != null){
            document.setFolder(null);
        }

        document.setUpdatedBy(currentUser);
        Document saved = documentRepository.save(document);
        if (request.getSubjectIds() != null){
            document.getDocumentSubjects().clear();
            documentSubjectRepository.deleteAll(document.getDocumentSubjects());
            if (!request.getSubjectIds().isEmpty()){
                assignSubjects(saved,request.getSubjectIds());
            }
        }

        if (request.getTagNames() != null) {
            // Remove existing
            document.getDocumentTags().clear();
            documentTagRepository.deleteAll(document.getDocumentTags());
            // Add new
            if (!request.getTagNames().isEmpty()) {
                assignTags(saved, request.getTagNames());
            }
        }
        // Log event
        logEvent(saved, DocumentEvent.DocumentEventType.UPDATED, "Document updated");

        logger.info("Document updated - id: {}, userId: {}", id, currentUserId);

        return convertToResponse(saved);
    }

    @Transactional
    public void deleteDocument(Long id){
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null){
            throw new RuntimeException("User not authenticated");
        }

        Document document = documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUser().getId().equals(currentUserId)){
            boolean isAdmin = document.getUser().getRoles().stream()
                    .anyMatch(role -> role.getName().equals("ADMIN"));
            if (!isAdmin){
                throw new RuntimeException("You don't have permission to delete this document");
            }
        }

        User currentUser = userRepository.findById((currentUserId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(currentUser);
        document.setStatus(Document.DocumentStatus.DELETED);

        documentRepository.save(document);
        logEvent(document, DocumentEvent.DocumentEventType.DELETED, "Document deleted");

        logger.info("Document deleted (soft) - id: {}, userId: {}",id,currentUserId);
    }

    @Transactional
    public DocumentResponse restoreDocument(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getDeletedAt() == null) {
            throw new RuntimeException("Document is not deleted");
        }

        // Check ownership
        if (!document.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You don't have permission to restore this document");
        }

        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setStatus(Document.DocumentStatus.DRAFT);

        Document saved = documentRepository.save(document);

        // Log event
        logEvent(saved, DocumentEvent.DocumentEventType.RESTORED, "Document restored");

        logger.info("Document restored - id: {}, userId: {}", id, currentUserId);

        return convertToResponse(saved);
    }

    public DocumentResponse getDocumentById(Long id){
        Document document =documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Long currentUserId = securityUtils.getCurrentUserId();
        // Allow access if: document is PUBLIC, OR user is the owner
        if (document.getVisibility() != Document.DocumentVisibility.PUBLIC 
            && (currentUserId == null || !document.getUser().getId().equals(currentUserId))){
            throw new RuntimeException("You don't have permission to access this document");
        }

        incrementViewCount(document);
        return convertToResponse(document);
    }

    public Page<DocumentResponse> getMyDocuments(String status, Long folderId, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }

        Page<Document> documents;

        if (folderId != null) {
            documents = documentRepository.findByUserIdAndFolderIdAndDeletedAtIsNull(
                    currentUserId, folderId, pageable);
        } else if (status != null) {
            try {
                Document.DocumentStatus docStatus = Document.DocumentStatus.valueOf(status);
                documents = documentRepository.findByUserIdAndStatusAndDeletedAtIsNull(
                        currentUserId, docStatus, pageable);
            } catch (IllegalArgumentException e) {
                documents = documentRepository.findByUserIdAndDeletedAtIsNull(currentUserId, pageable);
            }
        } else {
            documents = documentRepository.findByUserIdAndDeletedAtIsNull(currentUserId, pageable);
        }

        return documents.map(this::convertToResponse);
    }

    public Page<DocumentResponse> getPublicDocuments(String status, Pageable pageable) {
        Document.DocumentStatus docStatus = status != null ?
                Document.DocumentStatus.valueOf(status) : Document.DocumentStatus.PUBLISHED;

        Page<Document> documents = documentRepository.findByVisibilityAndStatusAndDeletedAtIsNull(
                Document.DocumentVisibility.PUBLIC, docStatus, pageable);

        return documents.map(this::convertToResponse);
    }
    public Page<DocumentResponse> searchDocuments(String keyword, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();

        Page<Document> documents;
        if (currentUserId != null) {
            // Search trong documents của user + public documents
            documents = documentRepository.searchByUserAndKeyword(currentUserId, keyword, pageable);
        } else {
            // Chỉ search public documents
            documents = documentRepository.searchByKeyword(keyword, pageable);
        }

        return documents.map(this::convertToResponse);
    }

    private  void assignSubjects(Document document, Set<Long> subjectIds) {
        for (Long subjectId : subjectIds){
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found " + subjectId));

            DocumentSubject docSubject = new DocumentSubject();
            docSubject.setDocument(document);
            docSubject.setSubject(subject);
            documentSubjectRepository.save(docSubject);
        }
    }
    private void assignTags(Document document, Set<String> tagNames){
        for (String tagName : tagNames){
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        newTag.setSlug(generateSlug(tagName));
                        return tagRepository.save(newTag);
                    });
            
            // Create the DocumentTag relation
            DocumentTag docTag = new DocumentTag();
            docTag.setDocument(document);
            docTag.setTag(tag);
            documentTagRepository.save(docTag);
        }
    }

    private String generateSlug(String name){
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private void logEvent(Document document, DocumentEvent.DocumentEventType eventType,String descripsion){
        if (documentEventService != null){
            Long userId = securityUtils.getCurrentUserId();
            String ip = securityUtils.getClientIp();
            String userAgent = securityUtils.getUserAgent();
            documentEventService.logEvent(
                    document.getId(),
                    userId,
                    eventType,
                    descripsion,
                    null,
                    null,
                    ip,
                    userAgent
            );
        }
    }

    @Transactional
    public void incrementViewCount(Document document){
        document.setViewCount(document.getViewCount() + 1);
        documentRepository.save(document);

        logEvent(document, DocumentEvent.DocumentEventType.VIEWED, "Document viewd");
    }
    private DocumentResponse convertToResponse(Document document){
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setUserId(document.getUser().getId());
        response.setUsername(document.getUser().getUsername());
        response.setTitle(document.getTitle());
        response.setDescription(document.getDescription());
        response.setContent(document.getContent());
        response.setFileUrl(document.getFileUrl());
        response.setFileName(document.getFileName());
        response.setFileSize(document.getFileSize());
        response.setFileType(document.getFileType());
        response.setThumbnailUrl(document.getThumbnailUrl());
        response.setStatus(document.getStatus().name());
        response.setVisibility(document.getVisibility().name());
        response.setIsFeatured(document.getIsFeatured());
        response.setViewCount(document.getViewCount());
        response.setDownloadCount(document.getDownloadCount());
        response.setFavoriteCount(document.getFavouriteCount());
        response.setRatingAverage(document.getRatingAverage());
        response.setRatingCount(document.getRatingCount());
        response.setVersionNumber(document.getVersionNumber());
        response.setLanguage(document.getLanguage());

        if (document.getParentDocument() != null){
            response.setParentDocumentId(document.getParentDocument().getId());
        }

        if (document.getFolder() != null){
            response.setFolderId(document.getFolder().getId());
            response.setFolderName(document.getFolder().getName());
        }

        Set<String> subjects = document.getDocumentSubjects().stream()
                .map(ds -> ds.getSubject().getName())
                .collect(Collectors.toSet());
        response.setSubjects(subjects);

        Set<String> tags = document.getDocumentTags().stream()
                .map(dt -> dt.getTag().getName())
                .collect(Collectors.toSet());
        response.setTags(tags);

        response.setCreatedAt(document.getCreatedAt());
        if (document.getCreatedBy() != null){
            response.setCreatedByUsername(document.getCreatedBy().getUsername());
        }
        response.setUpdatedAt(document.getUpdatedAt());
        if (document.getUpdatedBy() != null){
            response.setUpdatedByUsername(document.getUpdatedBy().getUsername());
        }
        return response;
    }
}
