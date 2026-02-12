package com.studydocs.manager.repository;

import com.studydocs.manager.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject,Long> {
    Optional<Subject> findByName(String name);
    Optional<Subject> findBySlug(String slug);
}
