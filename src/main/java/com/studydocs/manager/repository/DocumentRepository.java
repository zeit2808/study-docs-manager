package com.studydocs.manager.repository;
import com.studydocs.manager.enums.*;

import com.studydocs.manager.entity.Document;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

        // Find by user
        Page<Document> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

        // Find by user and status
        Page<Document> findByUserIdAndStatusAndDeletedAtIsNull(Long userId, DocumentStatus status, Pageable pageable);

        // Find public documents
        Page<Document> findByVisibilityAndStatusAndDeletedAtIsNull(
                        DocumentVisibility visibility,
                        DocumentStatus status,
                        Pageable pageable);

        // Find by folder (paginated)
        Page<Document> findByFolderIdAndDeletedAtIsNull(Long folderId, Pageable pageable);

        // Find by folder (all, for cascade delete / unlink)
        List<Document> findAllByFolderIdAndDeletedAtIsNull(Long folderId);

        List<Document> findAllByFolderId(Long folderId);

        List<Document> findAllByFolderIdIn(List<Long> folderIds);

        @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.asset " +
                        "WHERE d.user.id = :userId AND d.folder.id = :folderId AND d.deletedAt IS NULL")
        List<Document> findActiveByUserIdAndFolderIdWithAsset(@Param("userId") Long userId,
                        @Param("folderId") Long folderId);

        @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.asset " +
                        "WHERE d.user.id = :userId AND d.folder IS NULL AND d.deletedAt IS NULL")
        List<Document> findActiveRootByUserIdWithAsset(@Param("userId") Long userId);

        long countByFolderIdAndDeletedAtIsNull(Long folderId);

        // Find by user and folder
        Page<Document> findByUserIdAndFolderIdAndDeletedAtIsNull(Long userId, Long folderId, Pageable pageable);

        @Query("SELECT DISTINCT d FROM Document d " +
                        "LEFT JOIN FETCH d.asset " +
                        "LEFT JOIN FETCH d.user " +
                        "LEFT JOIN FETCH d.folder " +
                        "LEFT JOIN FETCH d.documentTags dt " +
                        "LEFT JOIN FETCH dt.tag " +
                        "LEFT JOIN FETCH d.documentSubjects ds " +
                        "LEFT JOIN FETCH ds.subject " +
                        "WHERE d.id = :id")
        Optional<Document> findByIdForSearchIndexing(@Param("id") Long id);

        // Find by status
        Page<Document> findByStatusAndDeletedAtIsNull(DocumentStatus status, Pageable pageable);

        // Find by visibility
        Page<Document> findByVisibilityAndDeletedAtIsNull(DocumentVisibility visibility, Pageable pageable);

        // Find not deleted
        Optional<Document> findByIdAndDeletedAtIsNull(Long id);
        List<Document> findByIdInAndDeletedAtIsNull(List<Long> ids);

        // Count by user
        long countByUserIdAndDeletedAtIsNull(Long userId);

        // Count by status
        long countByStatusAndDeletedAtIsNull(DocumentStatus status);

        // Find featured documents
        Page<Document> findByIsFeaturedTrueAndStatusAndDeletedAtIsNull(
                        DocumentStatus status,
                        Pageable pageable);

        // Find by date range
        @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startDate AND :endDate AND d.deletedAt IS NULL")
        Page<Document> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND d.visibility = :visibility AND d.deletedAt IS NULL")
        Page<Document> findByUserIdAndVisibility(@Param("userId") Long userId,
                        @Param("visibility") DocumentVisibility visibility, Pageable pageable);

        // Cleanup query: Find old deleted documents whose current asset still has a file
        @Query("SELECT d FROM Document d JOIN d.asset a " +
                        "WHERE d.deletedAt < :cutoffDate AND a.objectName IS NOT NULL")
        List<Document> findByDeletedAtBeforeAndAssetObjectNameIsNotNull(@Param("cutoffDate") LocalDateTime cutoffDate);

        // Paginated cleanup query: status=DELETED + deletedAt < cutoff + asset still has file
        @Query("SELECT d FROM Document d JOIN d.asset a " +
                        "WHERE d.status = :status AND d.deletedAt < :cutoffDate AND a.objectName IS NOT NULL")
        Page<Document> findByStatusAndDeletedAtBeforeAndAssetObjectNameIsNotNull(
                        @Param("status") DocumentStatus status,
                        @Param("cutoffDate") LocalDateTime cutoffDate,
                        Pageable pageable);

        Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

        List<Document> findByUserIdAndStatus(Long userId, DocumentStatus status);

        // Trash purge query: DELETED + asset removed or objectName already cleaned
        @Query("SELECT d FROM Document d LEFT JOIN d.asset a " +
                        "WHERE d.status = :status AND d.deletedAt < :cutoffDate " +
                        "AND d.deletedRootFolderId IS NULL " +
                        "AND (a IS NULL OR a.objectName IS NULL)")
        Page<Document> findByStatusAndDeletedAtBeforeAndAssetMissingOrObjectNameIsNull(
                        @Param("status") DocumentStatus status,
                        @Param("cutoffDate") LocalDateTime cutoffDate,
                        Pageable pageable);

        // Trash: paginated list of deleted documents for a specific user
        Page<Document> findByUserIdAndStatusAndDeletedAtIsNotNull(
                        Long userId, DocumentStatus status, Pageable pageable);

        List<Document> findByUserIdAndDeletedRootFolderId(Long userId, Long deletedRootFolderId);

        List<Document> findByDeletedRootFolderId(Long deletedRootFolderId);

}
