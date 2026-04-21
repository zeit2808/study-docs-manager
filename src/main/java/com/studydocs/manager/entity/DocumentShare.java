package com.studydocs.manager.entity;

import com.studydocs.manager.enums.SharePermission;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_shares", indexes = {
        @Index(name = "idx_document_shares_document_id", columnList = "document_id"),
        @Index(name = "idx_document_shares_shared_with", columnList = "shared_with"),
        @Index(name = "idx_document_shares_expires_at", columnList = "expires_at"),
        @Index(name = "idx_document_shares_revoked_at", columnList = "revoked_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_share", columnNames = { "document_id", "shared_with" })
})
public class DocumentShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by", nullable = false)
    private User sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with", nullable = false)
    private User sharedWith;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SharePermission permission = SharePermission.VIEW;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Thời điểm share bị thu hồi thủ công. Null = chưa revoke. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Share còn hiệu lực khi: chưa revoke VÀ chưa hết hạn.
     */
    public boolean isEffective() {
        return revokedAt == null
                && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public User getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(User sharedBy) {
        this.sharedBy = sharedBy;
    }

    public User getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(User sharedWith) {
        this.sharedWith = sharedWith;
    }

    public SharePermission getPermission() {
        return permission;
    }

    public void setPermission(SharePermission permission) {
        this.permission = permission;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public User getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(User revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
