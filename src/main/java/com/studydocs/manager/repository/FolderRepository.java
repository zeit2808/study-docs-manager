package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder,Long> {
    Optional<Folder> findByIdAndDeletedAtIsNull(Long id);
    List<Folder> findByIdInAndDeletedAtIsNull(List<Long> ids);
    List<Folder> findByUserIdAndParentIdIsNullAndDeletedAtIsNullOrderBySortOrder(Long userId);
    List<Folder> findByUserIdAndParentIdAndDeletedAtIsNullOrderBySortOrder(Long userId, Long parentId);
    List<Folder> findByParentIdOrderBySortOrder(Long parentId);
    boolean existsByUserIdAndNameAndParentIdAndDeletedAtIsNull(Long userId, String name, Long parentId);
    boolean existsByUserIdAndNameAndParentIdIsNullAndDeletedAtIsNull(Long userId, String name);
    long countByParentIdAndDeletedAtIsNull(Long parentId);
    List<Folder> findByParentIdAndDeletedAtIsNullOrderBySortOrder(Long parentId);
    List<Folder> findByUserIdAndDeletedRootFolderId(Long userId, Long deletedRootFolderId);

    @Query("SELECT DISTINCT f.deletedRootFolderId FROM Folder f WHERE f.user.id = :userId AND f.deletedAt IS NOT NULL AND f.deletedRootFolderId IS NOT NULL")
    List<Long> findDistinctDeletedRootFolderIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Folder f " +
            "WHERE f.user.id = :userId AND f.deletedAt IS NOT NULL AND f.deletedRootFolderId = f.id " +
            "ORDER BY f.deletedAt DESC")
    List<Folder> findDeletedRootFoldersByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Folder f " +
            "WHERE f.deletedAt IS NOT NULL AND f.deletedRootFolderId = f.id " +
            "ORDER BY f.deletedAt DESC")
    List<Folder> findDeletedRootFolders();

    @Query("SELECT f FROM Folder f " +
            "WHERE f.deletedAt IS NOT NULL AND f.deletedAt < :cutoffDate AND f.deletedRootFolderId = f.id " +
            "ORDER BY f.deletedAt ASC")
    List<Folder> findDeletedRootFoldersBefore(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
