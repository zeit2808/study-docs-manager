package com.studydocs.manager.service.folder;

import com.studydocs.manager.entity.Folder;
import com.studydocs.manager.entity.FolderEvent;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.FolderEventType;
import com.studydocs.manager.repository.FolderEventRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ghi lifecycle events của Folder vào bảng {@code folder_events}.
 *
 * <h3>Quy tắc logging (Decision Rule)</h3>
 * <pre>
 * folder_events  = lịch sử của từng folder (từ góc nhìn của folder đó)
 * audit_logs     = admin oversight, batch operations (có IP + UserAgent)
 * </pre>
 *
 * <table border="1">
 *   <tr><th>Hành động</th><th>folder_events</th><th>audit_logs</th></tr>
 *   <tr><td>Create (individual)</td><td>CREATED</td><td>—</td></tr>
 *   <tr><td>Rename (individual)</td><td>RENAMED</td><td>—</td></tr>
 *   <tr><td>Move (individual)</td><td>MOVED</td><td>—</td></tr>
 *   <tr><td>Move (batch)</td><td>MOVED</td><td>MOVE_FOLDER</td></tr>
 *   <tr><td>Copy (batch)</td><td>COPIED</td><td>COPY_FOLDER</td></tr>
 *   <tr><td>Delete (trash)</td><td>DELETED</td><td>—</td></tr>
 *   <tr><td>Restore</td><td>RESTORED</td><td>—</td></tr>
 * </table>
 *
 * <p>Batch Copy/Move ghi vào <b>cả hai</b> bảng với mục đích khác nhau:
 * {@code folder_events} phục vụ history timeline của folder;
 * {@code audit_logs} phục vụ admin audit với context targetFolder + IP.
 */
@Service
public class FolderEventService {

    private static final Logger logger = LoggerFactory.getLogger(FolderEventService.class);

    private final FolderEventRepository folderEventRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public FolderEventService(
            FolderEventRepository folderEventRepository,
            UserRepository userRepository,
            SecurityUtils securityUtils) {
        this.folderEventRepository = folderEventRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    /** Log khi folder được tạo mới (individual). */
    public void logCreated(Folder folder) {
        String newValue = buildFolderSnapshot(folder.getName(), folder.getParent());
        persist(folder, FolderEventType.CREATED, "Folder created", null, newValue);
    }

    /**
     * Log khi folder bị đổi tên (name thay đổi nhưng parent không đổi).
     *
     * @param folder   folder sau khi lưu (đã có tên mới)
     * @param oldName  tên trước khi thay đổi
     */
    public void logRenamed(Folder folder, String oldName) {
        String oldValue = buildFolderSnapshot(oldName, folder.getParent());
        String newValue = buildFolderSnapshot(folder.getName(), folder.getParent());
        persist(folder, FolderEventType.RENAMED,
                "Folder renamed from \"" + oldName + "\" to \"" + folder.getName() + "\"",
                oldValue, newValue);
    }

    /**
     * Log khi folder bị di chuyển sang parent khác.
     * <ul>
     *   <li>Individual move (UpdateFolderUseCase): chỉ ghi folder_events</li>
     *   <li>Batch move (MoveItemsUseCase): ghi folder_events + audit_logs (MOVE_FOLDER)</li>
     * </ul>
     *
     * @param folder    folder sau khi lưu (đã có parent mới)
     * @param oldParent parent cũ (null = root)
     */
    public void logMoved(Folder folder, Folder oldParent) {
        String oldValue = buildFolderSnapshot(folder.getName(), oldParent);
        String newValue = buildFolderSnapshot(folder.getName(), folder.getParent());
        persist(folder, FolderEventType.MOVED,
                "Folder moved from parent=" + parentId(oldParent) + " to parent=" + parentId(folder.getParent()),
                oldValue, newValue);
    }

    /**
     * Log khi folder được sao chép (batch copy qua CopyItemsUseCase).
     * Đồng thời, CopyItemsUseCase cũng ghi vào audit_logs (COPY_FOLDER) với targetFolder + IP context.
     *
     * @param copiedFolder folder bản sao mới được tạo
     * @param sourceFolder folder gốc được copy từ đó
     */
    public void logCopied(Folder copiedFolder, Folder sourceFolder) {
        String oldValue = buildFolderSnapshot(sourceFolder.getName(), sourceFolder.getParent());
        String newValue = buildFolderSnapshot(copiedFolder.getName(), copiedFolder.getParent());
        persist(copiedFolder, FolderEventType.COPIED,
                "Folder copied from folderId=" + sourceFolder.getId() + " (\"" + sourceFolder.getName() + "\")",
                oldValue, newValue);
    }

    /**
     * Log khi folder bị soft-delete (đưa vào trash).
     *
     * @param folder folder bị xóa
     */
    public void logDeleted(Folder folder) {
        String oldValue = buildFolderSnapshot(folder.getName(), folder.getParent());
        persist(folder, FolderEventType.DELETED, "Folder moved to trash", oldValue, null);
    }

    /**
     * Log khi folder được khôi phục khỏi trash.
     *
     * @param folder folder vừa được restore
     */
    public void logRestored(Folder folder) {
        String newValue = buildFolderSnapshot(folder.getName(), folder.getParent());
        persist(folder, FolderEventType.RESTORED, "Folder restored from trash", null, newValue);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void persist(Folder folder, FolderEventType eventType,
                         String description, String oldValue, String newValue) {
        try {
            Long userId = securityUtils.getCurrentUserId();
            User user = userId != null
                    ? userRepository.findById(userId).orElse(null)
                    : null;

            FolderEvent event = new FolderEvent();
            event.setFolder(folder);
            event.setUser(user);
            event.setEventType(eventType);
            event.setDescription(description);
            event.setOldValue(oldValue);
            event.setNewValue(newValue);
            folderEventRepository.save(event);
        } catch (Exception ex) {
            // Audit log thất bại không được làm gián đoạn business flow
            logger.warn("Failed to persist folder event [{}] for folderId={}: {}",
                    eventType, folder.getId(), ex.getMessage());
        }
    }

    /**
     * Tạo snapshot JSON đơn giản: {"name":"...","parentId":...}
     */
    private String buildFolderSnapshot(String name, Folder parent) {
        String escapedName = name != null ? name.replace("\"", "\\\"") : "";
        String parentIdValue = parent != null ? String.valueOf(parent.getId()) : "null";
        return "{\"name\":\"" + escapedName + "\",\"parentId\":" + parentIdValue + "}";
    }

    private String parentId(Folder parent) {
        return parent != null ? String.valueOf(parent.getId()) : "root";
    }
}
