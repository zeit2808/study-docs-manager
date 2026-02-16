package com.studydocs.manager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.studydocs.manager.entity.Document.DocumentStatus;
import com.studydocs.manager.entity.Document.DocumentVisibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO cho advanced document search
 */
public class DocumentSearchRequest {

    /**
     * Search query string - tìm kiếm trong title, description, content
     */
    private String query;

    /**
     * Filters
     */
    private List<DocumentStatus> statuses;
    private List<DocumentVisibility> visibilities;
    private List<String> tags;
    private List<Long> subjectIds;
    private Long authorId;
    private List<String> fileTypes;
    private String language;
    private Long folderId;
    private Boolean isFeatured;

    /**
     * Date range filters
     * Accepts both date-only format (yyyy-MM-dd) and datetime format
     * (yyyy-MM-dd'T'HH:mm:ss)
     */
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss]", lenient = OptBoolean.TRUE)
    private LocalDateTime dateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss]", lenient = OptBoolean.TRUE)
    private LocalDateTime dateTo;

    /**
     * Rating filter
     */
    private BigDecimal minRating;

    /**
     * Sort options
     */
    private SortOption sortBy = SortOption.RELEVANCE;
    private SortOrder sortOrder = SortOrder.DESC;

    /**
     * Pagination
     */
    private Integer page = 0;
    private Integer size = 20;

    /**
     * Search options
     */
    private Boolean fuzzySearch = true;
    private Boolean highlightResults = true;

    // Enums
    public enum SortOption {
        RELEVANCE, // Sort by search score
        DATE, // Sort by createdAt
        UPDATED, // Sort by updatedAt
        RATING, // Sort by ratingAverage
        VIEWS, // Sort by viewCount
        DOWNLOADS, // Sort by downloadCount
        FAVORITES, // Sort by favouriteCount
        TITLE // Sort by title alphabetically
    }

    public enum SortOrder {
        ASC, DESC
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<DocumentStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<DocumentStatus> statuses) {
        this.statuses = statuses;
    }

    public List<DocumentVisibility> getVisibilities() {
        return visibilities;
    }

    public void setVisibilities(List<DocumentVisibility> visibilities) {
        this.visibilities = visibilities;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<Long> getSubjectIds() {
        return subjectIds;
    }

    public void setSubjectIds(List<Long> subjectIds) {
        this.subjectIds = subjectIds;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }

    public BigDecimal getMinRating() {
        return minRating;
    }

    public void setMinRating(BigDecimal minRating) {
        this.minRating = minRating;
    }

    public SortOption getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortOption sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getFuzzySearch() {
        return fuzzySearch;
    }

    public void setFuzzySearch(Boolean fuzzySearch) {
        this.fuzzySearch = fuzzySearch;
    }

    public Boolean getHighlightResults() {
        return highlightResults;
    }

    public void setHighlightResults(Boolean highlightResults) {
        this.highlightResults = highlightResults;
    }
}
