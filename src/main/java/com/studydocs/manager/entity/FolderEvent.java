package com.studydocs.manager.entity;

import com.studydocs.manager.enums.FolderEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Ghi lại lifecycle event của folder: tạo, đổi tên, di chuyển, xóa, khôi phục.
 * <p>
 * Copy/Move folder qua FileManager được ghi riêng vào audit_logs (COPY_FOLDER, MOVE_FOLDER).
 * folder_id được giữ nullable để log không bị mất khi folder bị purge khỏi DB.
 */
@Entity
@Table(name = "folder_events", indexes = {
        @Index(name = "idx_folder_events_folder_id", columnList = "folder_id, created_at"),
        @Index(name = "idx_folder_events_user_id", columnList = "user_id"),
        @Index(name = "idx_folder_events_event_type", columnList = "event_type"),
        @Index(name = "idx_folder_events_created_at", columnList = "created_at")
})
public class FolderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Folder liên quan. Nullable: log vẫn được giữ sau khi folder bị purge.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = true)
    private Folder folder;

    /**
     * User thực hiện hành động.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FolderEventType eventType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Giá trị trước thay đổi (JSON). Ví dụ: {"name":"Old Name","parentId":5}
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * Giá trị sau thay đổi (JSON). Ví dụ: {"name":"New Name","parentId":10}
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

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

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FolderEventType getEventType() {
        return eventType;
    }

    public void setEventType(FolderEventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
