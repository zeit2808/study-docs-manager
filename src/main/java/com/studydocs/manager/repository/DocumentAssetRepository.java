package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, Long> {
    Optional<DocumentAsset> findByDocumentId(Long documentId);

    List<DocumentAsset> findByDocumentIdIn(List<Long> documentIds);

    boolean existsByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
