package com.studydocs.manager.application.subject.usecase;

import com.studydocs.manager.application.subject.SubjectMapper;
import com.studydocs.manager.dto.subject.SubjectCreateRequest;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.SubjectRepository;
import com.studydocs.manager.service.common.SlugService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CreateSubjectUseCase {


    private final SubjectRepository subjectRepository;
    private final SlugService slugService;

    public CreateSubjectUseCase(SubjectRepository subjectRepository, SlugService slugService) {
        this.subjectRepository = subjectRepository;
        this.slugService = slugService;
    }

    @Transactional
    public SubjectResponse execute(SubjectCreateRequest req) {
        String name = req.getName().trim();
        if (subjectRepository.existsByNameIgnoreCaseAndIsActiveTrue(name)) {
            throw new ConflictException("Subject name already exists", "SUBJECT_NAME_EXISTS", "name");
        }

        Subject s = new Subject();
        s.setName(name);
        s.setSlug(slugService.uniqueSlug(name, subjectRepository::existsBySlug));
        s.setDescription(req.getDescription());
        s.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        s.setIsActive(true);

        if (req.getParentId() != null) {
            Subject parent = subjectRepository.findByIdAndIsActiveTrue(req.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent subject not found", "PARENT_SUBJECT_NOT_FOUND", "parentId"));
            s.setParent(parent);
        }
        return SubjectMapper.toResponse(subjectRepository.save(s));
    }
}
