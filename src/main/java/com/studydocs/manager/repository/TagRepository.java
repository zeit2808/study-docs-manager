package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByIdAndIsActiveTrue(Long id);
    Optional<Tag> findByName(String name);
    Optional<Tag> findByNameIgnoreCase(String name);
    Optional<Tag> findBySlug(String slug);

    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);
    boolean existsBySlug(String slug);

    Page<Tag> findByIsActiveTrue(Pageable pageable);
    Page<Tag> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);
}
