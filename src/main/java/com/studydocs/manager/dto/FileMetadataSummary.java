package com.studydocs.manager.dto;

/**
 * FileMetadataSummary - Lightweight DTO chứa essential metadata fields
 * 
 * Sử dụng trong FileUploadResponse để giảm response size.
 * Chỉ chứa các fields cần thiết để pre-fill document creation form.
 * 
 * So với FileMetadata (full):
 * - Không có extractedText (quá lớn, 5000+ characters)
 * - Không có additionalMetadata (ít dùng)
 * - Không có description, subject (trùng lặp với title)
 * - Response size: ~200-500 bytes thay vì 5KB+
 */
public class FileMetadataSummary {

    private String title; // Document title - pre-fill vào title field
    private String author; // Tác giả - hiển thị info
    private String keywords; // Keywords - convert thành tags
    private String language; // Ngôn ngữ - pre-fill language dropdown
    private Integer pageCount; // Số trang - hiển thị info
    private Integer wordCount; // Số từ - hiển thị info

    // Constructors
    public FileMetadataSummary() {
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

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
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

    @Override
    public String toString() {
        return "FileMetadataSummary{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", keywords='" + keywords + '\'' +
                ", language='" + language + '\'' +
                ", pageCount=" + pageCount +
                ", wordCount=" + wordCount +
                '}';
    }
}
