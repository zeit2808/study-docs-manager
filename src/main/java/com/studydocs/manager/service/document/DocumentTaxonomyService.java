package com.studydocs.manager.service.document;

import com.studydocs.manager.entity.Document;
import com.studydocs.manager.entity.DocumentSubject;
import com.studydocs.manager.entity.DocumentTag;
import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.DocumentSubjectRepository;
import com.studydocs.manager.repository.DocumentTagRepository;
import com.studydocs.manager.repository.SubjectRepository;
import com.studydocs.manager.repository.TagRepository;
import com.studydocs.manager.service.common.SlugService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DocumentTaxonomyService {

    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;
    private final DocumentSubjectRepository documentSubjectRepository;
    private final DocumentTagRepository documentTagRepository;
    private final SlugService slugService;

    public DocumentTaxonomyService(
            SubjectRepository subjectRepository,
            TagRepository tagRepository,
            DocumentSubjectRepository documentSubjectRepository,
            DocumentTagRepository documentTagRepository,
            SlugService slugService) {
        this.subjectRepository = subjectRepository;
        this.tagRepository = tagRepository;
        this.documentSubjectRepository = documentSubjectRepository;
        this.documentTagRepository = documentTagRepository;
        this.slugService = slugService;
    }

    public void assignSubjects(Document document, Set<Long> subjectIds) {
        Set<Long> normalizedIds = normalizeSubjectIds(subjectIds);
        if (normalizedIds.isEmpty()) {
            return;
        }

        List<DocumentSubject> documentSubjects = new ArrayList<>();
        for (Long subjectId : normalizedIds) {
            Subject subject = subjectRepository.findByIdAndIsActiveTrue(subjectId)
                    .orElseThrow(() -> new NotFoundException(
                            "Subject not found " + subjectId,
                            "SUBJECT_NOT_FOUND",
                            "subjectIds"));

            DocumentSubject documentSubject = new DocumentSubject();
            documentSubject.setDocument(document);
            documentSubject.setSubject(subject);
            documentSubjects.add(documentSubject);

            document.getDocumentSubjects().add(documentSubject);
        }

        documentSubjectRepository.saveAll(documentSubjects);
    }

    public void assignTags(Document document, Set<String> tagNames) {
        Set<String> normalizedNames = normalizeTagNames(tagNames);
        if (normalizedNames.isEmpty()) {
            return;
        }

        List<DocumentTag> documentTags = new ArrayList<>();
        for (String tagName : normalizedNames) {
            Tag tag = resolveOrCreateTag(tagName);

            DocumentTag documentTag = new DocumentTag();
            documentTag.setDocument(document);
            documentTag.setTag(tag);
            documentTags.add(documentTag);

            document.getDocumentTags().add(documentTag);
        }

        documentTagRepository.saveAll(documentTags);
    }

    public void replaceSubjects(Document document, Set<Long> subjectIds) {
        Set<DocumentSubject> existing = new HashSet<>(document.getDocumentSubjects());
        document.getDocumentSubjects().clear();
        if (!existing.isEmpty()) {
            documentSubjectRepository.deleteAll(existing);
        }

        if (subjectIds != null && !subjectIds.isEmpty()) {
            assignSubjects(document, subjectIds);
        }
    }

    public void replaceTags(Document document, Set<String> tagNames) {
        Set<DocumentTag> existing = new HashSet<>(document.getDocumentTags());
        document.getDocumentTags().clear();
        if (!existing.isEmpty()) {
            documentTagRepository.deleteAll(existing);
        }

        if (tagNames != null && !tagNames.isEmpty()) {
            assignTags(document, tagNames);
        }
    }

    private Tag resolveOrCreateTag(String tagName) {
        return tagRepository.findByNameIgnoreCase(tagName)
                .map(existing -> {
                    if (Boolean.FALSE.equals(existing.getIsActive())) {
                        existing.setIsActive(true);
                        return tagRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Tag newTag = new Tag();
                    newTag.setName(tagName);
                    newTag.setSlug(slugService.uniqueSlug(tagName, tagRepository::existsBySlug));
                    newTag.setIsActive(true);
                    return tagRepository.save(newTag);
                });
    }

    private Set<Long> normalizeSubjectIds(Set<Long> subjectIds) {
        Set<Long> normalized = new LinkedHashSet<>();
        if (subjectIds == null) {
            return normalized;
        }

        for (Long subjectId : subjectIds) {
            if (subjectId == null || subjectId <= 0) {
                throw new BadRequestException(
                        "Subject id must be a positive number",
                        "INVALID_SUBJECT_ID",
                        "subjectIds");
            }
            normalized.add(subjectId);
        }

        return normalized;
    }

    private Set<String> normalizeTagNames(Set<String> tagNames) {
        Set<String> normalized = new LinkedHashSet<>();
        if (tagNames == null) {
            return normalized;
        }

        for (String tagName : tagNames) {
            if (tagName == null) {
                continue;
            }

            String cleaned = tagName.trim();
            if (cleaned.isEmpty()) {
                continue;
            }

            normalized.add(cleaned);
        }

        return normalized;
    }
}