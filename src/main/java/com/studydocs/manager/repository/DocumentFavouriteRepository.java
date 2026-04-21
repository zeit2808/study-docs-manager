package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentFavourite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentFavouriteRepository extends JpaRepository<DocumentFavourite, Long> {
    Optional<DocumentFavourite> findByDocumentIdAndUserId(Long documentId, Long userId);

    List<DocumentFavourite> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByDocumentId(Long documentId);
}
