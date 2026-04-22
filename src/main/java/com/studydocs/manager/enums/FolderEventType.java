package com.studydocs.manager.enums;

/**
 * Loại sự kiện lifecycle của folder.
 *
 * <p><b>Quy tắc logging:</b>
 * <ul>
 *   <li>{@code folder_events} — lịch sử của từng folder (user/admin có thể xem)</li>
 *   <li>{@code audit_logs}   — admin oversight cho batch copy/move (có IP, UserAgent)</li>
 *   <li>Batch Copy/Move ghi vào <b>cả hai</b> với mục đích khác nhau (xem {@link com.studydocs.manager.enums.AuditAction})</li>
 * </ul>
 */
public enum FolderEventType {
    /** Folder được tạo mới. */
    CREATED,
    /** Folder bị đổi tên (parent không đổi). */
    RENAMED,
    /** Folder bị di chuyển sang parent khác (individual move qua UpdateFolder). */
    MOVED,
    /** Folder được sao chép (batch copy qua CopyItemsUseCase). */
    COPIED,
    /** Folder bị đưa vào trash. */
    DELETED,
    /** Folder được khôi phục khỏi trash. */
    RESTORED
}
