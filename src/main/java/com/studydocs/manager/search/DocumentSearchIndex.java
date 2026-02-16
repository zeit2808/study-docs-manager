package com.studydocs.manager.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.studydocs.manager.entity.Document.DocumentStatus;
import com.studydocs.manager.entity.Document.DocumentVisibility;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch Document Index Entity
 * 
 * Lưu trữ document data trong Elasticsearch để hỗ trợ full-text search.
 * Sử dụng Vietnamese analyzer cho các text fields để tối ưu search cho tiếng
 * Việt.
 * 
 * Indexing strategy: Chỉ index documents có status = PUBLISHED
 * Content limit: 10,000 characters preview
 */
@Document(indexName = "documents")
@Setting(settingPath = "elasticsearch-settings.json")
public class DocumentSearchIndex {

    @Id
    private Long id;

    /**
     * Title - searchable với boost cao (quan trọng nhất)
     * Analyzer: Vietnamese để hỗ trợ tách từ tiếng Việt
     */
    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type)
    })
    private String title;

    /**
     * Description - searchable với boost trung bình
     */
    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer")
    private String description;

    /**
     * Extracted text content từ file (PDF, Word, etc.)
     * Giới hạn 10,000 characters
     */
    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer")
    private String content;

    // File information
    @Field(type = FieldType.Keyword)
    private String fileName;

    @Field(type = FieldType.Keyword)
    private String fileType;

    @Field(type = FieldType.Long)
    private Long fileSize;

    @Field(type = FieldType.Keyword)
    private String objectName;

    // Author information
    @Field(type = FieldType.Long)
    private Long authorId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword) })
    private String authorName;

    @Field(type = FieldType.Keyword)
    private String authorUsername;

    // Categorization
    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Long)
    private List<Long> subjectIds;

    @Field(type = FieldType.Keyword)
    private List<String> subjectNames;

    // Folder
    @Field(type = FieldType.Long)
    private Long folderId;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer"), otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword) })
    private String folderName;

    // Status and visibility
    @Field(type = FieldType.Keyword)
    private DocumentStatus status;

    @Field(type = FieldType.Keyword)
    private DocumentVisibility visibility;

    @Field(type = FieldType.Boolean)
    private Boolean isFeatured;

    // Language
    @Field(type = FieldType.Keyword)
    private String language;

    // Statistics - for sorting and filtering
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer downloadCount;

    @Field(type = FieldType.Integer)
    private Integer favouriteCount;

    @Field(type = FieldType.Double)
    private Double ratingAverage;

    @Field(type = FieldType.Integer)
    private Integer ratingCount;

    // Dates - for filtering and sorting
    // Use Instant instead of LocalDateTime for better Elasticsearch compatibility
    @Field(type = FieldType.Date, format = {}, pattern = "strict_date_optional_time_nanos||strict_date_optional_time||epoch_millis")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant createdAt;

    @Field(type = FieldType.Date, format = {}, pattern = "strict_date_optional_time_nanos||strict_date_optional_time||epoch_millis")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant updatedAt;

    @Field(type = FieldType.Date, format = {}, pattern = "strict_date_optional_time_nanos||strict_date_optional_time||epoch_millis")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant indexedAt; // Track when document was indexed

    // Thumbnail
    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    // Constructors
    public DocumentSearchIndex() {
        this.indexedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        // Limit to 10,000 characters
        if (content != null && content.length() > 10000) {
            this.content = content.substring(0, 10000);
        } else {
            this.content = content;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
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

    public List<String> getSubjectNames() {
        return subjectNames;
    }

    public void setSubjectNames(List<String> subjectNames) {
        this.subjectNames = subjectNames;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DocumentVisibility visibility) {
        this.visibility = visibility;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(Integer favouriteCount) {
        this.favouriteCount = favouriteCount;
    }

    public Double getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(Double ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(Instant indexedAt) {
        this.indexedAt = indexedAt;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
