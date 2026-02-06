package com.studydocs.manager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subjects", indexes = {
        @Index(name = "idx_subjects_parent_id", columnList = "parent_id"),
        @Index(name = "idx_subjects_slug", columnList = "slug"),
        @Index(name = "idx_subjects_is_active", columnList = "is_active, sort_order")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_subject_slug", columnNames = "slug")
})

public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Subject parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private Set<Subject> children = new HashSet<>();

    @Column(name = "icon_url", length = 1000)
    private String iconUrl;

    @Column(length = 20)
    private String color;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DocumentSubject> documentSubjects = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Subject getParent() { return parent; }
    public void setParent(Subject parent) { this.parent = parent; }

    public Set<Subject> getChildren() { return children; }
    public void setChildren(Set<Subject> children) { this.children = children; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Integer getDocumentCount() { return documentCount; }
    public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<DocumentSubject> getDocumentSubjects() { return documentSubjects; }
    public void setDocumentSubjects(Set<DocumentSubject> documentSubjects) { this.documentSubjects = documentSubjects; }
}
