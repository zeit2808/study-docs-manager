package com.studydocs.manager.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import com.studydocs.manager.config.SearchProperties;
import com.studydocs.manager.dto.document.DocumentSearchRequest;
import com.studydocs.manager.dto.document.DocumentSearchResponse;
import com.studydocs.manager.dto.document.DocumentSearchResult;
import com.studydocs.manager.enums.DocumentStatus;
import com.studydocs.manager.enums.DocumentVisibility;
import com.studydocs.manager.enums.SortOption;
import com.studydocs.manager.enums.SortOrder;
import com.studydocs.manager.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "search.indexing.enabled", havingValue = "true", matchIfMissing = false)
public class DocumentSearchService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSearchService.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final DocumentSearchRepository searchRepository;
    private final SearchProperties searchProperties;
    private final StorageProvider storageProvider;

    public DocumentSearchService(ElasticsearchOperations elasticsearchOperations,
            DocumentSearchRepository searchRepository,
            SearchProperties searchProperties,
            StorageProvider storageProvider) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.searchRepository = searchRepository;
        this.searchProperties = searchProperties;
        this.storageProvider = storageProvider;
    }

    public DocumentSearchResponse searchDocuments(DocumentSearchRequest request) {
        long startTime = System.currentTimeMillis();
        DocumentSearchRequest safeRequest = request != null ? request : new DocumentSearchRequest();

        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            applyPublicSearchPolicy(boolQueryBuilder);

            if (safeRequest.getQuery() != null && !safeRequest.getQuery().trim().isEmpty()) {
                boolQueryBuilder.must(buildTextSearchQuery(safeRequest));
            }

            applyFilters(boolQueryBuilder, safeRequest);

            Query finalQuery = boolQueryBuilder.build()._toQuery();
            NativeQuery nativeQuery = buildNativeQuery(finalQuery, safeRequest);
            SearchHits<DocumentSearchIndex> searchHits = elasticsearchOperations.search(
                    nativeQuery,
                    DocumentSearchIndex.class);

            DocumentSearchResponse response = convertToResponse(searchHits, safeRequest);
            response.setQuery(safeRequest.getQuery());
            response.setSearchTimeMs(System.currentTimeMillis() - startTime);
            return response;
        } catch (Exception e) {
            logger.error("Search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            applyPublicSearchPolicy(boolQueryBuilder);
            boolQueryBuilder.must(MatchPhrasePrefixQuery.of(m -> m
                    .field("title.suggest")
                    .query(prefix))._toQuery());

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(boolQueryBuilder.build()._toQuery())
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

    public List<DocumentSearchResult> findSimilarDocuments(Long documentId, int limit) {
        try {
            Optional<DocumentSearchIndex> optionalDoc = searchRepository.findById(documentId);
            if (optionalDoc.isEmpty()) {
                return Collections.emptyList();
            }

            DocumentSearchIndex source = optionalDoc.get();
            if (source.getStatus() != DocumentStatus.PUBLISHED || source.getVisibility() != DocumentVisibility.PUBLIC) {
                return Collections.emptyList();
            }

            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            applyPublicSearchPolicy(boolQueryBuilder);
            boolQueryBuilder.must(MoreLikeThisQuery.of(m -> m
                    .fields(Arrays.asList("title", "description", "tags"))
                    .like(l -> l.document(d -> d
                            .index("documents")
                            .id(documentId.toString())))
                    .minTermFreq(1)
                    .maxQueryTerms(12))._toQuery());
            boolQueryBuilder.mustNot(q -> q.term(t -> t.field("id").value(FieldValue.of(documentId))));

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(boolQueryBuilder.build()._toQuery())
                    .withPageable(PageRequest.of(0, Math.max(1, Math.min(limit, 20))))
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

    private void applyPublicSearchPolicy(BoolQuery.Builder boolQueryBuilder) {
        boolQueryBuilder.filter(f -> f.term(t -> t.field("status").value(FieldValue.of(DocumentStatus.PUBLISHED.name()))));
        boolQueryBuilder.filter(
                f -> f.term(t -> t.field("visibility").value(FieldValue.of(DocumentVisibility.PUBLIC.name()))));
    }

    private Query buildTextSearchQuery(DocumentSearchRequest request) {
        boolean fuzzySearch = request.getFuzzySearch() != null
                ? request.getFuzzySearch()
                : searchProperties.isFuzzyEnabled();

        if (fuzzySearch) {
            return MultiMatchQuery.of(m -> m
                    .query(request.getQuery())
                    .fields("title^3", "description^2", "content")
                    .fuzziness(searchProperties.getFuzzyFuzziness())
                    .prefixLength(2))._toQuery();
        }

        return MultiMatchQuery.of(m -> m
                .query(request.getQuery())
                .fields("title^3", "description^2", "content"))._toQuery();
    }

    private void applyFilters(BoolQuery.Builder boolQueryBuilder, DocumentSearchRequest request) {
        if (request.getAuthorId() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("authorId")
                    .value(FieldValue.of(request.getAuthorId()))));
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("tags")
                    .terms(tv -> tv.value(request.getTags().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("subjectIds")
                    .terms(tv -> tv.value(request.getSubjectIds().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
            boolQueryBuilder.filter(f -> f.terms(t -> t
                    .field("fileType")
                    .terms(tv -> tv.value(request.getFileTypes().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList())))));
        }

        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("language")
                    .value(FieldValue.of(request.getLanguage()))));
        }

        if (request.getFolderId() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("folderId")
                    .value(FieldValue.of(request.getFolderId()))));
        }

        if (request.getIsFeatured() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("isFeatured")
                    .value(FieldValue.of(request.getIsFeatured()))));
        }

        if (request.getDateFrom() != null || request.getDateTo() != null) {
            RangeQuery rangeQuery = RangeQuery.of(rq -> rq.date(dr -> {
                dr.field("createdAt");
                if (request.getDateFrom() != null) {
                    Instant fromInstant = request.getDateFrom().toInstant(ZoneOffset.UTC);
                    dr.gte(fromInstant.toString());
                }
                if (request.getDateTo() != null) {
                    Instant toInstant = request.getDateTo().toInstant(ZoneOffset.UTC);
                    dr.lte(toInstant.toString());
                }
                return dr;
            }));

            boolQueryBuilder.filter(q -> q.range(rangeQuery));
        }

        if (request.getMinRating() != null) {
            RangeQuery ratingRange = RangeQuery.of(rq -> rq.number(nr -> nr
                    .field("ratingAverage")
                    .gte(request.getMinRating().doubleValue())));
            boolQueryBuilder.filter(q -> q.range(ratingRange));
        }
    }

    private NativeQuery buildNativeQuery(Query query, DocumentSearchRequest request) {
        int page = request.getPage() != null ? Math.max(0, request.getPage()) : 0;
        int requestedSize = request.getSize() != null ? request.getSize() : 20;
        int size = Math.max(1, Math.min(requestedSize, searchProperties.getMaxPageSize()));

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(page, size))
                .withSort(buildSort(request));

        boolean highlight = request.getHighlightResults() != null
                ? request.getHighlightResults()
                : searchProperties.isHighlightEnabled();
        if (highlight) {
            builder.withHighlightQuery(buildHighlightQuery());
        }

        return builder.build();
    }

    private Sort buildSort(DocumentSearchRequest request) {
        SortOption sortBy = request.getSortBy() != null ? request.getSortBy() : SortOption.RELEVANCE;
        Sort.Direction direction = request.getSortOrder() == SortOrder.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (sortBy) {
            case DATE -> Sort.by(direction, "createdAt");
            case UPDATED -> Sort.by(direction, "updatedAt");
            case RATING -> Sort.by(direction, "ratingAverage");
            case FAVORITES -> Sort.by(direction, "favouriteCount");
            case TITLE -> Sort.by(direction, "title.keyword");
            case RELEVANCE -> Sort.by(Sort.Order.desc("_score"));
        };
    }

    private HighlightQuery buildHighlightQuery() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags(searchProperties.getHighlightPreTag())
                .withPostTags(searchProperties.getHighlightPostTag())
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

    private DocumentSearchResponse convertToResponse(SearchHits<DocumentSearchIndex> searchHits,
            DocumentSearchRequest request) {
        int page = request.getPage() != null ? Math.max(0, request.getPage()) : 0;
        int requestedSize = request.getSize() != null ? request.getSize() : 20;
        int size = Math.max(1, Math.min(requestedSize, searchProperties.getMaxPageSize()));

        List<DocumentSearchResult> results = searchHits.getSearchHits().stream()
                .map(this::convertToResult)
                .collect(Collectors.toList());

        return new DocumentSearchResponse(results, searchHits.getTotalHits(), page, size);
    }

    private DocumentSearchResult convertToResult(SearchHit<DocumentSearchIndex> hit) {
        DocumentSearchIndex index = hit.getContent();
        DocumentSearchResult result = new DocumentSearchResult();

        result.setDocumentId(index.getId());
        result.setTitle(index.getTitle());
        result.setDescription(index.getDescription());
        result.setScore(hit.getScore());

        Map<String, List<String>> highlights = hit.getHighlightFields();
        result.setHighlights(highlights);
        result.setAuthorId(index.getAuthorId());
        result.setAuthorName(index.getAuthorName());
        result.setAuthorUsername(index.getAuthorUsername());
        result.setFileName(index.getFileName());
        result.setFileType(index.getFileType());
        result.setFileSize(index.getFileSize());
        result.setThumbnailUrl(generateUrl(index.getThumbnailObjectName()));
        result.setTags(index.getTags());
        result.setSubjectNames(index.getSubjectNames());
        result.setFolderName(index.getFolderName());
        result.setStatus(index.getStatus());
        result.setVisibility(index.getVisibility());
        result.setIsFeatured(index.getIsFeatured());
        result.setLanguage(index.getLanguage());
        result.setFavouriteCount(index.getFavouriteCount());
        result.setRatingAverage(index.getRatingAverage());
        result.setRatingCount(index.getRatingCount());
        result.setCreatedAt(index.getCreatedAt() != null
                ? LocalDateTime.ofInstant(index.getCreatedAt(), ZoneOffset.UTC)
                : null);
        result.setUpdatedAt(index.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(index.getUpdatedAt(), ZoneOffset.UTC)
                : null);

        return result;
    }

    private String generateUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        try {
            return storageProvider.generatePresignedUrl(objectName, 7 * 24 * 60);
        } catch (IOException e) {
            logger.warn("Failed to generate thumbnail URL for object: {}", objectName, e);
            return null;
        }
    }
}
