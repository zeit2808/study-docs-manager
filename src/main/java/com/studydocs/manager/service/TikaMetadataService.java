package com.studydocs.manager.service;

import com.studydocs.manager.dto.FileMetadata;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * TikaMetadataService - Service sử dụng Apache Tika để extract metadata từ file
 * 
 * Hỗ trợ nhiều loại file: PDF, Word (DOC/DOCX), Excel (XLS/XLSX), PowerPoint
 * (PPT/PPTX), TXT, etc.
 * 
 * Sử dụng trong two-step upload pattern:
 * 1. Extract metadata từ file upload
 * 2. Trả metadata cho frontend để pre-fill form
 * 3. User review và submit document creation
 */
@Service
public class TikaMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(TikaMetadataService.class);

    // Giới hạn text extraction để tránh OutOfMemory
    private static final int MAX_TEXT_LENGTH = 1_000_000; // 1MB text

    /**
     * Extract metadata và text content từ file
     * 
     * @param file MultipartFile từ upload
     * @return FileMetadata chứa tất cả metadata extracted
     */
    public FileMetadata extractMetadata(MultipartFile file) {
        FileMetadata fileMetadata = new FileMetadata();

        try (InputStream inputStream = file.getInputStream()) {
            // Initialize Tika components
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            // Set filename để Tika detect tốt hơn
            metadata.set("resourceName", file.getOriginalFilename());

            // Parse file để extract metadata và content
            logger.debug("Parsing file: {}", file.getOriginalFilename());
            parser.parse(inputStream, handler, metadata, context);

            // Extract basic metadata (using string literals for compatibility)
            fileMetadata.setTitle(getMetadataValue(metadata, "dc:title"));
            fileMetadata.setAuthor(getMetadataValue(metadata, "dc:creator"));
            fileMetadata.setSubject(getMetadataValue(metadata, "dc:subject"));
            fileMetadata.setKeywords(getMetadataValue(metadata, "meta:keyword"));
            fileMetadata.setDescription(getMetadataValue(metadata, "dc:description"));

            // Extract technical details
            fileMetadata.setContentType(getMetadataValue(metadata, "Content-Type"));
            fileMetadata.setLanguage(getMetadataValue(metadata, "dc:language"));

            // Extract dates
            fileMetadata.setCreationDate(getMetadataValue(metadata, "dcterms:created"));
            fileMetadata.setModificationDate(getMetadataValue(metadata, "dcterms:modified"));

            // Extract page count (for PDF, Word, etc.)
            String pageCountStr = getMetadataValue(metadata, "xmpTPg:NPages");
            if (pageCountStr == null) {
                pageCountStr = getMetadataValue(metadata, "meta:page-count");
            }
            if (pageCountStr != null) {
                try {
                    fileMetadata.setPageCount(Integer.parseInt(pageCountStr));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse page count: {}", pageCountStr);
                }
            }

            // Extract word count
            String wordCountStr = getMetadataValue(metadata, "meta:word-count");
            if (wordCountStr != null) {
                try {
                    fileMetadata.setWordCount(Integer.parseInt(wordCountStr));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse word count: {}", wordCountStr);
                }
            }

            // Extract text content (preview - limited by MAX_TEXT_LENGTH)
            String extractedText = handler.toString();
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                fileMetadata.setExtractedText(extractedText.trim());
            }

            // Store all metadata as additional info
            Map<String, String> additionalMetadata = new HashMap<>();
            for (String name : metadata.names()) {
                String value = metadata.get(name);
                if (value != null && !value.trim().isEmpty()) {
                    additionalMetadata.put(name, value);
                }
            }
            fileMetadata.setAdditionalMetadata(additionalMetadata);

            logger.info("Metadata extracted successfully for: {} - Title: {}, Pages: {}, Words: {}",
                    file.getOriginalFilename(),
                    fileMetadata.getTitle(),
                    fileMetadata.getPageCount(),
                    fileMetadata.getWordCount());

        } catch (Exception e) {
            logger.error("Error extracting metadata from file: {} - {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to extract metadata: " + e.getMessage(), e);
        }

        return fileMetadata;
    }

    /**
     * Extract lightweight metadata summary từ file
     * 
     * Chỉ extract essential fields, không extract text content và additional
     * metadata
     * để giảm response size và processing time.
     * 
     * @param file MultipartFile từ upload
     * @return FileMetadataSummary chứa essential metadata fields
     */
    public com.studydocs.manager.dto.FileMetadataSummary extractMetadataSummary(MultipartFile file) {
        // Extract full metadata first
        FileMetadata fullMetadata = extractMetadata(file);

        // Convert to summary (only essential fields)
        com.studydocs.manager.dto.FileMetadataSummary summary = new com.studydocs.manager.dto.FileMetadataSummary();
        summary.setTitle(fullMetadata.getTitle());
        summary.setAuthor(fullMetadata.getAuthor());
        summary.setKeywords(fullMetadata.getKeywords());
        summary.setLanguage(fullMetadata.getLanguage());
        summary.setPageCount(fullMetadata.getPageCount());
        summary.setWordCount(fullMetadata.getWordCount());

        logger.debug("Metadata summary created - Title: {}, Pages: {}, Words: {}",
                summary.getTitle(), summary.getPageCount(), summary.getWordCount());

        return summary;
    }

    /**
     * Extract chỉ text content từ file (không extract metadata)
     * Hữu ích khi chỉ cần nội dung để index hoặc search
     * 
     * @param file      MultipartFile từ upload
     * @param maxLength Giới hạn độ dài text (characters)
     * @return Text content extracted
     */
    /**
     * Detect MIME type của file
     * 
     * @param file MultipartFile
     * @return MIME type (e.g., "application/pdf")
     */
    public String detectMimeType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Tika tika = new Tika();
            return tika.detect(inputStream, file.getOriginalFilename());
        } catch (Exception e) {
            logger.error("Error detecting MIME type for file: {}", file.getOriginalFilename());
            return file.getContentType(); // Fallback to provided content type
        }
    }

    /**
     * Helper method để lấy metadata value, trả về null nếu không có hoặc empty
     */
    private String getMetadataValue(Metadata metadata, org.apache.tika.metadata.Property property) {
        String value = metadata.get(property);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    /**
     * Helper method để lấy metadata value theo tên
     */
    private String getMetadataValue(Metadata metadata, String name) {
        String value = metadata.get(name);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    public String extractText(InputStream inputStream, int maxLength, String resourceName) {
        if (inputStream == null)
            return null;

        ContentHandler handler = new BodyContentHandler(maxLength); // Declare handler outside try for scope

        try {
            Parser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            if (resourceName != null && !resourceName.isBlank()) {
                metadata.set("resourceName", resourceName);

            }
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();

        } catch (org.apache.tika.exception.WriteLimitReachedException e) {
            // This is NOT an error - it means we successfully extracted up to the limit
            logger.debug("Content extraction reached limit of {} characters for {}. Extracted text up to limit.",
                    maxLength, resourceName);
            // The handler still contains the text up to the limit, so return it
            return handler.toString();
        } catch (Exception e) {
            logger.error("Error extracting text from stream: {} - {}", resourceName, e.getMessage(), e);
            return null;
        }
    }

    // tiện dùng khi không cần resourceName
    public String extractText(InputStream inputStream, int maxLength) {
        return extractText(inputStream, maxLength, null);
    }
}
