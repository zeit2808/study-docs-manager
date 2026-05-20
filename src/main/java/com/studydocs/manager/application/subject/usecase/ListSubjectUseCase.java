package com.studydocs.manager.application.subject.usecase;

import com.studydocs.manager.application.subject.SubjectMapper;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.repository.SubjectRepository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListSubjectUseCase {

    private final SubjectRepository subjectRepository;
    public ListSubjectUseCase(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public Page<SubjectResponse> execute(Pageable pageable) {
        return subjectRepository.findByIsActiveTrue(pageable)
                .map(SubjectMapper::toResponse);
    }

    public Page<SubjectResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return execute(pageable);
        }
        return subjectRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword.trim(), pageable)
                .map(SubjectMapper::toResponse);
    }
}