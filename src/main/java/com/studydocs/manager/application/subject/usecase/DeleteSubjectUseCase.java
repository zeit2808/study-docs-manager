package com.studydocs.manager.application.subject.usecase;

import com.studydocs.manager.entity.Subject;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.SubjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DeleteSubjectUseCase {

    private final SubjectRepository subjectRepository;

    public DeleteSubjectUseCase(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public void execute(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Subject not found",
                        "SUBJECT_NOT_FOUND",
                        "id"));

        if (Boolean.FALSE.equals(subject.getIsActive())) {
            return;
        }

        subject.setIsActive(false);
        subjectRepository.save(subject);
    }
}