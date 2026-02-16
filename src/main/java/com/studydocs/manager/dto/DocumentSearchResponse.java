package com.studydocs.manager.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO cho document search results
 */
public class DocumentSearchResponse {

    /**
     * Search results
     */
    private List<DocumentSearchResult> results;

    /**
     * Total number of matching documents
     */
    private long totalHits;

    /**
     * Pagination info
     */
    private int page;
    private int size;
    private int totalPages;

    /**
     * Aggregations/Facets - để hiển thị filter counts
     */
    private SearchAggregations aggregations;

    /**
     * Search metadata
     */
    private String query;
    private long searchTimeMs;

    // Constructor
    public DocumentSearchResponse() {
    }

    public DocumentSearchResponse(List<DocumentSearchResult> results, long totalHits, int page, int size) {
        this.results = results;
        this.totalHits = totalHits;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) totalHits / size);
    }

    // Getters and Setters
    public List<DocumentSearchResult> getResults() {
        return results;
    }

    public void setResults(List<DocumentSearchResult> results) {
        this.results = results;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public SearchAggregations getAggregations() {
        return aggregations;
    }

    public void setAggregations(SearchAggregations aggregations) {
        this.aggregations = aggregations;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getSearchTimeMs() {
        return searchTimeMs;
    }

    public void setSearchTimeMs(long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }

    /**
     * Nested class for search aggregations/facets
     */
    public static class SearchAggregations {
        private Map<String, Long> tagCounts;
        private Map<String, Long> subjectCounts;
        private Map<String, Long> fileTypeCounts;
        private Map<String, Long> authorCounts;

        public Map<String, Long> getTagCounts() {
            return tagCounts;
        }

        public void setTagCounts(Map<String, Long> tagCounts) {
            this.tagCounts = tagCounts;
        }

        public Map<String, Long> getSubjectCounts() {
            return subjectCounts;
        }

        public void setSubjectCounts(Map<String, Long> subjectCounts) {
            this.subjectCounts = subjectCounts;
        }

        public Map<String, Long> getFileTypeCounts() {
            return fileTypeCounts;
        }

        public void setFileTypeCounts(Map<String, Long> fileTypeCounts) {
            this.fileTypeCounts = fileTypeCounts;
        }

        public Map<String, Long> getAuthorCounts() {
            return authorCounts;
        }

        public void setAuthorCounts(Map<String, Long> authorCounts) {
            this.authorCounts = authorCounts;
        }
    }
}
