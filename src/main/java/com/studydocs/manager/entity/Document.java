package com.studydocs.manager.entity;

import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_user_deleted_created", columnList = "user_id, deleted_at, created_at"),
        @Index(name = "idx_documents_user_status_deleted", columnList = "user_id, status, deleted_at"),
        @Index(name = "idx_documents_user_folder_deleted", columnList = "user_id, folder_id, deleted_at"),
        @Index(name = "idx_documents_user_folder_display_deleted", columnList = "user_id, folder_id, normalized_display_name, deleted_at"),
        @Index(name = "idx_documents_visibility_status_deleted", columnList = "visibility, status, deleted_at"),
        @Index(name = "idx_documents_cleanup_status_deleted", columnList = "status, deleted_at"),
        @Index(name = "idx_documents_rating_average", columnList = "rating_average"),
        @Index(name = "idx_documents_deleted_root_folder", columnList = "deleted_root_folder_id")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_name", length = 500)
    private String displayName;

    @Column(name = "normalized_display_name", length = 500)
    private String normalizedDisplayName;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DocumentVisibility visibility = DocumentVisibility.PRIVATE;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "favorite_count")
    private Integer favouriteCount = 0;

    @Column(name = "rating_average", precision = 3, scale = 2)
    private java.math.BigDecimal ratingAverage = java.math.BigDecimal.ZERO;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Column(name = "version_number")
    private Integer versionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(length = 10)
    private String language = "vi";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "deleted_root_folder_id")
    private Long deletedRootFolderId;

    // Relationships
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentSubject> documentSubjects = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentTag> documentTags = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentEvent> documentEvents = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentDailyStat> documentDailyStats = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentFavourite> documentFavourites = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentRating> documentRatings = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentShare> documentShares = new HashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentVersion> documentVersions = new HashSet<>();

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DocumentAsset asset;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (createdBy == null && user != null) {
            createdBy = user;
        }
        normalizedDisplayName = normalizeDisplayName(displayName);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizedDisplayName = normalizeDisplayName(displayName);
    }

    public void markDeleted(User actor, Long deletedRootFolderId, LocalDateTime deletedTime) {
        this.deletedAt = deletedTime != null ? deletedTime : LocalDateTime.now();
        this.deletedBy = actor;
        this.deletedRootFolderId = deletedRootFolderId;
        this.status = DocumentStatus.DELETED;
    }

    public void restoreFromTrash(String restoredDisplayName) {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletedRootFolderId = null;
        this.status = DocumentStatus.DRAFT;
        if (restoredDisplayName != null) {
            this.displayName = restoredDisplayName;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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

    public String getNormalizedDisplayName() {
        return normalizedDisplayName;
    }

    public void setNormalizedDisplayName(String normalizedDisplayName) {
        this.normalizedDisplayName = normalizedDisplayName;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(Integer favoriteCount) {
        this.favouriteCount = favoriteCount;
    }

    public java.math.BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(java.math.BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    public Long getDeletedRootFolderId() {
        return deletedRootFolderId;
    }

    public void setDeletedRootFolderId(Long deletedRootFolderId) {
        this.deletedRootFolderId = deletedRootFolderId;
    }

    public Set<DocumentSubject> getDocumentSubjects() {
        return documentSubjects;
    }

    public void setDocumentSubjects(Set<DocumentSubject> documentSubjects) {
        this.documentSubjects = documentSubjects;
    }

    public Set<DocumentTag> getDocumentTags() {
        return documentTags;
    }

    public void setDocumentTags(Set<DocumentTag> documentTags) {
        this.documentTags = documentTags;
    }

    public Set<DocumentEvent> getDocumentEvents() {
        return documentEvents;
    }

    public void setDocumentEvents(Set<DocumentEvent> documentEvents) {
        this.documentEvents = documentEvents;
    }

    public Set<DocumentDailyStat> getDocumentDailyStats() {
        return documentDailyStats;
    }

    public void setDocumentDailyStats(Set<DocumentDailyStat> documentDailyStats) {
        this.documentDailyStats = documentDailyStats;
    }

    public Set<DocumentFavourite> getDocumentFavourites() {
        return documentFavourites;
    }

    public void setDocumentFavourites(Set<DocumentFavourite> documentFavourites) {
        this.documentFavourites = documentFavourites;
    }

    public Set<DocumentRating> getDocumentRatings() {
        return documentRatings;
    }

    public void setDocumentRatings(Set<DocumentRating> documentRatings) {
        this.documentRatings = documentRatings;
    }

    public Set<DocumentShare> getDocumentShares() {
        return documentShares;
    }

    public void setDocumentShares(Set<DocumentShare> documentShares) {
        this.documentShares = documentShares;
    }

    public Set<DocumentVersion> getDocumentVersions() {
        return documentVersions;
    }

    public void setDocumentVersions(Set<DocumentVersion> documentVersions) {
        this.documentVersions = documentVersions;
    }

    public DocumentAsset getAsset() {
        return asset;
    }

    public void setAsset(DocumentAsset asset) {
        this.asset = asset;
        if (asset != null && asset.getDocument() != this) {
            asset.setDocument(this);
        }
    }

    private String normalizeDisplayName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

}
