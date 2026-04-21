package com.studydocs.manager.enums;

/** Action types được ghi nhận trong audit_logs. */
public enum AuditAction {
    CREATE_USER_BY_ADMIN,
    UPDATE_USER,
    UPDATE_PROFILE,
    DELETE_USER,
    CHANGE_PASSWORD,
    RESET_PASSWORD,
    LOCK_USER,
    UNLOCK_USER,
    CHANGE_ROLE,
    COPY_DOCUMENT,
    MOVE_DOCUMENT,
    COPY_FOLDER,
    MOVE_FOLDER
}
