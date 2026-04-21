package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRatingRepository extends JpaRepository<DocumentRating, Long> {
    Optional<DocumentRating> findByDocumentIdAndUserId(Long documentId, Long userId);

    List<DocumentRating> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    long countByDocumentId(Long documentId);
}
