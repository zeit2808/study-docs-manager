package com.studydocs.manager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "folders", indexes = {
        @Index(name = "idx_folders_user_parent_deleted", columnList = "user_id, parent_id, deleted_at"),
        @Index(name = "idx_folders_user_parent_normalized_deleted", columnList = "user_id, parent_id, normalized_name, deleted_at"),
        @Index(name = "idx_folders_parent_id", columnList = "parent_id"),
        @Index(name = "idx_folders_deleted_root", columnList = "deleted_root_folder_id")
})
public class Folder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", length = 200)
    private String normalizedName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    @OneToMany(mappedBy = "parent")
    private Set<Folder> children = new HashSet<>();

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @Column(name = "deleted_root_folder_id")
    private Long deletedRootFolderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "folder")
    private Set<Document> documents = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        normalizedName = normalizeName(name);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizedName = normalizeName(name);
    }

    public void markDeleted(User actor, Long deletedRootFolderId, LocalDateTime deletedTime) {
        this.deletedAt = deletedTime != null ? deletedTime : LocalDateTime.now();
        this.deletedBy = actor;
        this.deletedRootFolderId = deletedRootFolderId;
    }

    public void restoreFromTrash() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletedRootFolderId = null;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public Folder getParent() {
        return parent;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public Set<Folder> getChildren() {
        return children;
    }

    public void setChildren(Set<Folder> children) {
        this.children = children;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(Set<Document> documents) {
        this.documents = documents;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

}
