package com.studydocs.manager.application.subject.usecase;

import com.studydocs.manager.application.subject.SubjectMapper;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.SubjectRepository;
import org.springframework.stereotype.Service;

@Service
public class GetSubjectUseCase {

    private final SubjectRepository subjectRepository;

    public GetSubjectUseCase(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public SubjectResponse execute(Long id) {
        Subject subject = subjectRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(
                        "Subject not found",
                        "SUBJECT_NOT_FOUND",
                        "id"));
        return SubjectMapper.toResponse(subject);
    }
}