package com.studydocs.manager.search;

import com.studydocs.manager.entity.Document.DocumentStatus;
import com.studydocs.manager.entity.Document.DocumentVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch Repository for DocumentSearchIndex
 * 
 * Cung cấp các method để query Elasticsearch index
 * Sử dụng Spring Data Elasticsearch để tự động generate queries
 */
@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentSearchIndex, Long> {

    /**
     * Tìm documents theo status
     */
    Page<DocumentSearchIndex> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * Tìm documents theo visibility
     */
    Page<DocumentSearchIndex> findByVisibility(DocumentVisibility visibility, Pageable pageable);

    /**
     * Tìm documents theo author
     */
    Page<DocumentSearchIndex> findByAuthorId(Long authorId, Pageable pageable);

    /**
     * Tìm documents theo tag
     */
    Page<DocumentSearchIndex> findByTagsContaining(String tag, Pageable pageable);

    /**
     * Tìm documents theo subject ID
     */
    Page<DocumentSearchIndex> findBySubjectIdsContaining(Long subjectId, Pageable pageable);

    /**
     * Tìm documents có isFeatured = true
     */
    Page<DocumentSearchIndex> findByIsFeatured(Boolean isFeatured, Pageable pageable);

    /**
     * Tìm documents trong khoảng thời gian
     */
    Page<DocumentSearchIndex> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    /**
     * Tìm documents theo file type
     */
    Page<DocumentSearchIndex> findByFileType(String fileType, Pageable pageable);

    /**
     * Tìm documents theo language
     */
    Page<DocumentSearchIndex> findByLanguage(String language, Pageable pageable);

    /**
     * Autocomplete suggestions cho title
     */
    @Query("{\"match_phrase_prefix\": {\"title\": \"?0\"}}")
    List<DocumentSearchIndex> findTitleSuggestions(String prefix);

    /**
     * Tìm documents theo multiple statuses
     */
    Page<DocumentSearchIndex> findByStatusIn(List<DocumentStatus> statuses, Pageable pageable);

    /**
     * Tìm documents theo multiple visibilities
     */
    Page<DocumentSearchIndex> findByVisibilityIn(List<DocumentVisibility> visibilities, Pageable pageable);

    /**
     * Count documents theo status
     */
    long countByStatus(DocumentStatus status);

    /**
     * Count documents theo visibility
     */
    long countByVisibility(DocumentVisibility visibility);

    /**
     * Delete by document ID
     */
    void deleteById(Long id);

    /**
     * Check if document exists
     */
    boolean existsById(Long id);
}
