package com.studydocs.manager.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Aggregate daily view/download stats per document.
 * Upsert 1 row/ngày/document thay vì insert từng lần vào document_events.
 * <p>
 * Tương đương: INSERT ... ON DUPLICATE KEY UPDATE view_count = view_count + 1
 */
@Entity
@Table(name = "document_daily_stats", indexes = {
        @Index(name = "idx_daily_stats_document_date", columnList = "document_id, stat_date"),
        @Index(name = "idx_daily_stats_stat_date", columnList = "stat_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_stats_doc_date", columnNames = { "document_id", "stat_date" })
})
public class DocumentDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "download_count", nullable = false)
    private Long downloadCount = 0L;

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }
}
