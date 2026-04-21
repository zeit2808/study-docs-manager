package com.studydocs.manager.enums;

/**
 * Loại sự kiện lifecycle của document.
 * VIEWED/DOWNLOADED đã được bỏ → dùng DocumentDailyStat để aggregate.
 */
public enum DocumentEventType {
    CREATED,
    COPIED,
    UPDATED,
    MOVED,
    DELETED,
    RESTORED,
    VERSION_CREATED,
    VERSION_RESTORED,
    PUBLISHED,
    ARCHIVED,
    SHARED,
    RATED,
    SUBJECT_ADDED,
    SUBJECT_REMOVED,
    TAG_ADDED,
    TAG_REMOVED
}
