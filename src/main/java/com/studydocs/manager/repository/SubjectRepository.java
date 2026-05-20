package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject,Long> {
    Optional<Subject> findByIdAndIsActiveTrue(Long id);
    Optional<Subject> findByNameIgnoreCase(String name);
    Optional<Subject> findBySlug(String slug);

    boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);
    boolean existsBySlug(String slug);

    Page<Subject> findByIsActiveTrue(Pageable pageable);
    Page<Subject> findByNameContainingIgnoreCaseAndIsActiveTrue(String keyword, Pageable pageable);
}
