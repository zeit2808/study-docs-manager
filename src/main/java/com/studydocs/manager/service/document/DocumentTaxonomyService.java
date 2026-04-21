package com.studydocs.manager.service.document;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentSubject;
import com.studydocs.manager.entity.DocumentTag;
import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentSubjectRepository;
import com.studydocs.manager.repository.DocumentTagRepository;
import com.studydocs.manager.repository.SubjectRepository;
import com.studydocs.manager.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages document classification: subjects and tags.
 * Extracted from DocumentService to satisfy Single Responsibility Principle.
 */
@Service
public class DocumentTaxonomyService {

    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;
    private final DocumentSubjectRepository documentSubjectRepository;
    private final DocumentTagRepository documentTagRepository;

    public DocumentTaxonomyService(
            SubjectRepository subjectRepository,
            TagRepository tagRepository,
            DocumentSubjectRepository documentSubjectRepository,
            DocumentTagRepository documentTagRepository) {
        this.subjectRepository = subjectRepository;
        this.tagRepository = tagRepository;
        this.documentSubjectRepository = documentSubjectRepository;
        this.documentTagRepository = documentTagRepository;
    }

    /**
     * Assigns a set of subjects to a document.
     */
    public void assignSubjects(Document document, Set<Long> subjectIds) {
        for (Long subjectId : subjectIds) {
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new NotFoundException(
                            "Subject not found " + subjectId, "SUBJECT_NOT_FOUND", "subjectIds"));

            DocumentSubject docSubject = new DocumentSubject();
            docSubject.setDocument(document);
            docSubject.setSubject(subject);
            documentSubjectRepository.save(docSubject);
        }
    }

    /**
     * Assigns a set of tags to a document. Creates new tags if they don't exist.
     */
    public void assignTags(Document document, Set<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(tagName);
                        newTag.setSlug(generateSlug(tagName));
                        return tagRepository.save(newTag);
                    });

            DocumentTag docTag = new DocumentTag();
            docTag.setDocument(document);
            docTag.setTag(tag);
            documentTagRepository.save(docTag);
        }
    }

    /**
     * Replaces all subjects on a document with a new set.
     * Clears existing subjects first, then assigns the new ones.
     */
    public void replaceSubjects(Document document, Set<Long> subjectIds) {
        Set<DocumentSubject> existing = new HashSet<>(document.getDocumentSubjects());
        document.getDocumentSubjects().clear();
        documentSubjectRepository.deleteAll(existing);
        if (subjectIds != null && !subjectIds.isEmpty()) {
            assignSubjects(document, subjectIds);
        }
    }

    /**
     * Replaces all tags on a document with a new set.
     * Clears existing tags first, then assigns the new ones.
     */
    public void replaceTags(Document document, Set<String> tagNames) {
        Set<DocumentTag> existing = new HashSet<>(document.getDocumentTags());
        document.getDocumentTags().clear();
        documentTagRepository.deleteAll(existing);
        if (tagNames != null && !tagNames.isEmpty()) {
            assignTags(document, tagNames);
        }
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
