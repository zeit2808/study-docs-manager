package com.studydocs.manager.dto.document;

import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request DTO for updating document metadata.
 *
 * <p><b>File asset fields</b> (objectName, fileName, fileSize, fileType,
 * thumbnailObjectName, content) are intentionally excluded — they are set by
 * the upload/processing pipeline and are not manually editable. To replace
 * the underlying file, use the dedicated file-replacement endpoint.
 */
public class DocumentUpdateRequest {

    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Display name must not exceed 500 characters")
    private String displayName;

    private String status;     // DRAFT, PUBLISHED, ARCHIVED

    private String visibility; // PRIVATE, PUBLIC, SHARED

    private Boolean isFeatured;

    private String language;

    private Set<Long> subjectIds;

    private Set<String> tagNames;

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<Long> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(Set<Long> subjectIds) {
        this.subjectIds = subjectIds;
    }

    public Set<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(Set<String> tagNames) {
        this.tagNames = tagNames;
    }
}
