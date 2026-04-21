package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {
    Optional<DocumentShare> findByDocumentIdAndSharedWithId(Long documentId, Long sharedWithId);

    List<DocumentShare> findBySharedWithIdAndRevokedAtIsNull(Long sharedWithId);

    List<DocumentShare> findByDocumentIdAndRevokedAtIsNullAndExpiresAtAfter(Long documentId, LocalDateTime currentTime);
}
