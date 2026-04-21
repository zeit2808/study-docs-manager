package com.studydocs.manager.repository;

import com.studydocs.manager.entity.DocumentDailyStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentDailyStatRepository extends JpaRepository<DocumentDailyStat, Long> {
    Optional<DocumentDailyStat> findByDocumentIdAndStatDate(Long documentId, LocalDate statDate);

    List<DocumentDailyStat> findByDocumentIdOrderByStatDateDesc(Long documentId);
}
