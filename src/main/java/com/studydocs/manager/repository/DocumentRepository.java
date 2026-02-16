package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Document;

import com.studydocs.manager.entity.Document.DocumentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.studydocs.manager.entity.Document.DocumentStatus;

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

        // Find by folder
        Page<Document> findByFolderIdAndDeletedAtIsNull(Long folderId, Pageable pageable);

        // Find by user and folder
        Page<Document> findByUserIdAndFolderIdAndDeletedAtIsNull(Long userId, Long folderId, Pageable pageable);

        // Search by title/description
        @Query("SELECT d FROM Document d WHERE " +
                        "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                        "d.deletedAt IS NULL")
        Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        // Search by user and keyword
        @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND " +
                        "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                        "d.deletedAt IS NULL")
        Page<Document> searchByUserAndKeyword(@Param("userId") Long userId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        // Find by status
        Page<Document> findByStatusAndDeletedAtIsNull(DocumentStatus status, Pageable pageable);

        // Find by visibility
        Page<Document> findByVisibilityAndDeletedAtIsNull(DocumentVisibility visibility, Pageable pageable);

        // Find not deleted
        Optional<Document> findByIdAndDeletedAtIsNull(Long id);

        // Count by user
        long countByUserIdAndDeletedAtIsNull(Long userId);

        // Count by status
        long countByStatusAndDeletedAtIsNull(DocumentStatus status);

        // Find featured documents
        Page<Document> findByIsFeaturedTrueAndStatusAndDeletedAtIsNull(
                        Document.DocumentStatus status,
                        Pageable pageable);

        // Find by date range
        @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startDate AND :endDate AND d.deletedAt IS NULL")
        Page<Document> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT d FROM Document d WHERE d.user.id = :userId AND d.visibility = :visibility AND d.deletedAt IS NULL")
        Page<Document> findByUserIdAndVisibility(@Param("userId") Long userId,
                        @Param("visibility") Document.DocumentVisibility visibility, Pageable pageable);

        // Cleanup query: Find old deleted documents with files
        List<Document> findByDeletedAtBeforeAndObjectNameIsNotNull(LocalDateTime cutoffDate);
}