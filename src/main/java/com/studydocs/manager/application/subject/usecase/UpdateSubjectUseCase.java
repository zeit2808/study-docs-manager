package com.studydocs.manager.application.subject.usecase;

import com.studydocs.manager.application.subject.SubjectMapper;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.dto.subject.SubjectUpdateRequest;
import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.SubjectRepository;
import com.studydocs.manager.service.common.SlugService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UpdateSubjectUseCase {

    private final SubjectRepository subjectRepository;
    private final SlugService slugService;

    public UpdateSubjectUseCase(SubjectRepository subjectRepository, SlugService slugService) {
        this.subjectRepository = subjectRepository;
        this.slugService = slugService;
    }

    @Transactional
    public SubjectResponse execute(Long id, SubjectUpdateRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Subject not found",
                        "SUBJECT_NOT_FOUND",
                        "id"));

        if (request.getName() != null) {
            String normalizedName = normalizeName(request.getName());
            boolean nameChanged = !normalizedName.equalsIgnoreCase(subject.getName());

            if (nameChanged && subjectRepository.existsByNameIgnoreCaseAndIsActiveTrue(normalizedName)) {
                throw new ConflictException("Subject name already exists", "SUBJECT_NAME_EXISTS", "name");
            }

            if (nameChanged) {
                subject.setName(normalizedName);
                subject.setSlug(slugService.uniqueSlug(normalizedName, slug -> {
                    if (slug.equals(subject.getSlug())) {
                        return false;
                    }
                    return subjectRepository.existsBySlug(slug);
                }));
            }
        }

        if (request.getDescription() != null) {
            subject.setDescription(normalizeNullableText(request.getDescription()));
        }

        if (request.getSortOrder() != null) {
            subject.setSortOrder(request.getSortOrder());
        }

        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(subject.getId())) {
                throw new BadRequestException(
                        "Subject cannot be its own parent",
                        "INVALID_SUBJECT_PARENT",
                        "parentId");
            }

            Subject parent = subjectRepository.findByIdAndIsActiveTrue(request.getParentId())
                    .orElseThrow(() -> new NotFoundException(
                            "Parent subject not found",
                            "PARENT_SUBJECT_NOT_FOUND",
                            "parentId"));

            subject.setParent(parent);
        }

        Subject saved = subjectRepository.save(subject);
        return SubjectMapper.toResponse(saved);
    }

    private String normalizeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Subject name must not be blank", "INVALID_SUBJECT_NAME", "name");
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}