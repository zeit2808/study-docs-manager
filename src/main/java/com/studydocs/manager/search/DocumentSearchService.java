package com.studydocs.manager.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.studydocs.manager.dto.DocumentSearchRequest;
import com.studydocs.manager.dto.DocumentSearchResponse;
import com.studydocs.manager.dto.DocumentSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import co.elastic.clients.json.JsonData;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để thực hiện advanced search trên Elasticsearch
 */
@Service
public class DocumentSearchService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSearchService.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private DocumentSearchRepository searchRepository;

    /**
     * Advanced document search với filters, sorting và highlighting
     */
    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Build query
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 1. Text search query (nếu có query string)
            if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
                Query textQuery = buildTextSearchQuery(request.getQuery(), request.getFuzzySearch());
                boolQueryBuilder.must(textQuery);
            }

            // 2. Apply filters
            applyFilters(boolQueryBuilder, request);

            // 3. Build final query
            Query finalQuery = boolQueryBuilder.build()._toQuery();

            // 4. Build NativeQuery với sorting
            NativeQuery nativeQuery = buildNativeQuery(finalQuery, request);

            // 5. Execute search
            SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(
                    nativeQuery,
                    DocumentSearchIndex.class);

            // 6. Convert to response
            DocumentSearchResponse response = convertToResponse(searchHits, request);
            response.setQuery(request.getQuery());
            response.setSearchTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Search completed: query='{}', hits={}, time={}ms",
                    request.getQuery(), searchHits.getTotalHits(), response.getSearchTimeMs());

            return response;

        } catch (Exception e) {
            logger.error("Search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Autocomplete suggestions cho title
     */
    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Use match_phrase_prefix for autocomplete
            Query autocompleteQuery = MatchPhrasePrefixQuery.of(m -> m
                    .field("title.suggest")
                    .query(prefix))._toQuery();

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(autocompleteQuery)
                    .withPageable(PageRequest.of(0, 10))
                    .build();

            SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(
                    nativeQuery,
                    DocumentSearchIndex.class);

            return searchHits.getSearchHits().stream()
                    .map(hit -> hit.getContent().getTitle())
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Autocomplete failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Find similar documents (More Like This query)
     */
    public List<DocumentSearchResult> findSimilarDocuments(Long documentId, int limit) {
        try {
            // Fetch the original document
            Optional<DocumentSearchIndex> optionalDoc = searchRepository.findById(documentId);
            if (optionalDoc.isEmpty()) {
                logger.warn("Document {} not found in search index", documentId);
                return Collections.emptyList();
            }

            // Build More Like This query
            Query mltQuery = MoreLikeThisQuery.of(m -> m
                    .fields(Arrays.asList("title", "description", "tags"))
                    .like(l -> l.document(d -> d
                            .index("documents")
                            .id(documentId.toString())))
                    .minTermFreq(1)
                    .maxQueryTerms(12))._toQuery();

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(mltQuery)
                    .withPageable(PageRequest.of(0, limit))
                    .build();

            SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(
                    nativeQuery,
                    DocumentSearchIndex.class);

            return searchHits.getSearchHits().stream()
                    .map(this::convertToResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Find similar documents failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Build text search query với multi-field matching và boosting
     */
    private Query buildTextSearchQuery(String queryText, Boolean fuzzySearch) {
        if (fuzzySearch != null && fuzzySearch) {
            // Fuzzy multi-match query cho typo tolerance
            return MultiMatchQuery.of(m -> m
                    .query(queryText)
                    .fields("title^3", "description^2", "content^1")
                    .fuzziness("AUTO")
                    .prefixLength(2))._toQuery();
        } else {
            // Standard multi-match query
            return MultiMatchQuery.of(m -> m
                    .query(queryText)
                    .fields("title^3", "description^2", "content^1"))._toQuery();
        }
    }

    /**
     * Apply filters vào BoolQuery
     */
    private void applyFilters(BoolQuery.Builder boolQueryBuilder, DocumentSearchRequest request) {
        // Status filter
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            List<String> statusValues = request.getStatuses().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());

            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("status")
                    .terms(tv -> tv.value(statusValues.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        // Visibility filter
        if (request.getVisibilities() != null && !request.getVisibilities().isEmpty()) {
            List<String> visibilityValues = request.getVisibilities().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());

            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("visibility")
                    .terms(tv -> tv.value(visibilityValues.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        // Author filter
        if (request.getAuthorId() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("authorId")
                    .value(FieldValue.of(request.getAuthorId()))));
        }

        // Tags filter (match any tag)
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("tags")
                    .terms(tv -> tv.value(request.getTags().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        // Subject IDs filter
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("subjectIds")
                    .terms(tv -> tv.value(request.getSubjectIds().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        // File types filter
        if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("fileType")
                    .terms(tv -> tv.value(request.getFileTypes().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        // Language filter
        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("language")
                    .value(FieldValue.of(request.getLanguage()))));
        }

        // Folder filter
        if (request.getFolderId() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("folderId")
                    .value(FieldValue.of(request.getFolderId()))));
        }

        // Featured filter
        if (request.getIsFeatured() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("isFeatured")
                    .value(FieldValue.of(request.getIsFeatured()))));
        }

        // Date range filter (createdAt is Instant type in Elasticsearch)
        if (request.getDateFrom() != null || request.getDateTo() != null) {

            RangeQuery rangeQuery = RangeQuery.of(rq -> rq.date(dr -> {
                dr.field("createdAt");

                if (request.getDateFrom() != null) {
                    // Convert LocalDateTime to Instant
                    Instant fromInstant = request.getDateFrom().toInstant(ZoneOffset.UTC);
                    dr.gte(fromInstant.toString()); // ISO-8601 string
                }
                if (request.getDateTo() != null) {
                    // Convert LocalDateTime to Instant
                    Instant toInstant = request.getDateTo().toInstant(ZoneOffset.UTC);
                    dr.lte(toInstant.toString());
                }

                return dr;
            }));

            boolQueryBuilder.filter(q -> q.range(rangeQuery));
        }

        // Rating filter (ratingAverage là number)
        if (request.getMinRating() != null) {
            double min = request.getMinRating().doubleValue(); // BigDecimal -> double

            RangeQuery ratingRange = RangeQuery.of(rq -> rq.number(nr -> nr
                    .field("ratingAverage")
                    .gte(min)));

            boolQueryBuilder.filter(q -> q.range(ratingRange));
        }
    }

    /**
     * Build NativeQuery với sorting và highlighting
     */
    private NativeQuery buildNativeQuery(Query query, DocumentSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(page, size))
                .withSort(buildSort(request)); // giờ sẽ hết lỗi

        if (Boolean.TRUE.equals(request.getHighlightResults())) {
            builder.withHighlightQuery(buildHighlightQuery());
        }

        return builder.build();
    }

    /**
     * Build sort based on request
     */
    private Sort buildSort(DocumentSearchRequest request) {
        String sortField;
        Sort.Direction direction = request.getSortOrder() == DocumentSearchRequest.SortOrder.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        switch (request.getSortBy()) {
            case DATE:
                sortField = "createdAt";
                break;
            case UPDATED:
                sortField = "updatedAt";
                break;
            case RATING:
                sortField = "ratingAverage";
                break;
            case VIEWS:
                sortField = "viewCount";
                break;
            case DOWNLOADS:
                sortField = "downloadCount";
                break;
            case FAVORITES:
                sortField = "favouriteCount";
                break;
            case TITLE:
                sortField = "title.keyword";
                break;
            case RELEVANCE:
            default:
                return Sort.by(Sort.Order.desc("_score"));
        }

        return Sort.by(direction, sortField);
    }

    /**
     * Build highlight query
     */
    private HighlightQuery buildHighlightQuery() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<em class=\"highlight\">")
                .withPostTags("</em>")
                .withNumberOfFragments(3)
                .withFragmentSize(150)
                .build();

        Highlight highlight = new Highlight(
                params,
                Arrays.asList(
                        new HighlightField("title"),
                        new HighlightField("description"),
                        new HighlightField("content")));

        return new HighlightQuery(highlight, DocumentSearchIndex.class);
    }

    /**
     * Convert SearchHits to DocumentSearchResponse
     */
    private DocumentSearchResponse convertToResponse(SearchHits<DocumentSearchIndex> searchHits,
            DocumentSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        List<DocumentSearchResult> results = searchHits.getSearchHits().stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());

        return new DocumentSearchResponse(results, searchHits.getTotalHits(), page, size);
    }

    /**
     * Convert single SearchHit to DocumentSearchResult
     */
    private DocumentSearchResult convertToResult(SearchHit<DocumentSearchIndex> hit) {
        DocumentSearchIndex index = hit.getContent();
        DocumentSearchResult result = new DocumentSearchResult();

        result.setDocumentId(index.getId());
        result.setTitle(index.getTitle());
        result.setDescription(index.getDescription());
        result.setScore(hit.getScore());

        // Extract highlights
        Map<String, List<String>> highlights = hit.getHighlightFields();
        result.setHighlights(highlights);

        // Author
        result.setAuthorId(index.getAuthorId());
        result.setAuthorName(index.getAuthorName());
        result.setAuthorUsername(index.getAuthorUsername());

        // File info
        result.setFileName(index.getFileName());
        result.setFileType(index.getFileType());
        result.setFileSize(index.getFileSize());
        result.setThumbnailUrl(index.getThumbnailUrl());

        // Categories
        result.setTags(index.getTags());
        result.setSubjectNames(index.getSubjectNames());
        result.setFolderName(index.getFolderName());

        // Status
        result.setStatus(index.getStatus());
        result.setVisibility(index.getVisibility());
        result.setIsFeatured(index.getIsFeatured());
        result.setLanguage(index.getLanguage());

        // Statistics
        result.setViewCount(index.getViewCount());
        result.setDownloadCount(index.getDownloadCount());
        result.setFavouriteCount(index.getFavouriteCount());
        result.setRatingAverage(index.getRatingAverage());
        result.setRatingCount(index.getRatingCount());

        // Dates - Convert Instant back to LocalDateTime for API response
        result.setCreatedAt(index.getCreatedAt() != null
                ? LocalDateTime.ofInstant(index.getCreatedAt(), ZoneOffset.UTC)
                : null);
        result.setUpdatedAt(index.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(index.getUpdatedAt(), ZoneOffset.UTC)
                : null);

        return result;
    }
}
