package com.studydocs.manager.entity;

import com.studydocs.manager.enums.DocumentEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Ghi lại lifecycle event của document: thay đổi trạng thái, chia sẻ, rating...
 * <p>
 * KHÔNG dùng để log VIEWED/DOWNLOADED (quá nhiều → bùng nổ bảng).
 * View/Download tracking → DocumentDailyStat (aggregate theo ngày).
 */
@Entity
@Table(name = "document_events", indexes = {
        @Index(name = "idx_document_events_document_id", columnList = "document_id, created_at"),
        @Index(name = "idx_document_events_user_id", columnList = "user_id"),
        @Index(name = "idx_document_events_event_type", columnList = "event_type"),
        @Index(name = "idx_document_events_created_at", columnList = "created_at")
})
public class DocumentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentEventType eventType;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // JSON format

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON format

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DocumentEventType getEventType() {
        return eventType;
    }

    public void setEventType(DocumentEventType eventType) {
        this.eventType = eventType;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
