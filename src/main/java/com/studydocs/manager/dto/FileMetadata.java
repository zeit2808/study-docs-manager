package com.studydocs.manager.dto;

import java.util.Map;

/**
 * FileMetadata - DTO chứa metadata được extract từ file bằng Apache Tika
 * 
 * Sử dụng trong two-step upload pattern:
 * - Step 1: Upload file → Nhận FileMetadata
 * - Step 2: Frontend pre-fill form với metadata → User review → Create document
 */
public class FileMetadata {

    // Basic metadata
    private String title; // Tiêu đề document
    private String author; // Tác giả
    private String subject; // Chủ đề
    private String keywords; // Từ khóa (có thể dùng làm tags)
    private String description; // Mô tả

    // Content
    private String extractedText; // Nội dung text được extract (preview)

    // Technical details
    private String contentType; // MIME type detected by Tika
    private Integer pageCount; // Số trang (PDF, Word, etc.)
    private Integer wordCount; // Số từ
    private String language; // Ngôn ngữ (en, vi, etc.)

    // Timestamps
    private String creationDate; // Ngày tạo file
    private String modificationDate; // Ngày chỉnh sửa cuối

    // Additional metadata
    private Map<String, String> additionalMetadata; // Metadata khác từ Tika

    // Constructors
    public FileMetadata() {
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
    }

    public Map<String, String> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, String> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", subject='" + subject + '\'' +
                ", keywords='" + keywords + '\'' +
                ", contentType='" + contentType + '\'' +
                ", pageCount=" + pageCount +
                ", wordCount=" + wordCount +
                ", language='" + language + '\'' +
                '}';
    }
}
