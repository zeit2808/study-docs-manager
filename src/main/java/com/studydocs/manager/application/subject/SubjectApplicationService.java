package com.studydocs.manager.application.subject;

import com.studydocs.manager.application.subject.usecase.CreateSubjectUseCase;
import com.studydocs.manager.application.subject.usecase.DeleteSubjectUseCase;
import com.studydocs.manager.application.subject.usecase.GetSubjectUseCase;
import com.studydocs.manager.application.subject.usecase.ListSubjectUseCase;
import com.studydocs.manager.application.subject.usecase.UpdateSubjectUseCase;
import com.studydocs.manager.dto.subject.SubjectCreateRequest;
import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.dto.subject.SubjectUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SubjectApplicationService {

    private final CreateSubjectUseCase createSubjectUseCase;
    private final GetSubjectUseCase getSubjectUseCase;
    private final ListSubjectUseCase listSubjectUseCase;
    private final UpdateSubjectUseCase updateSubjectUseCase;
    private final DeleteSubjectUseCase deleteSubjectUseCase;

    public SubjectApplicationService(
            CreateSubjectUseCase createSubjectUseCase,
            GetSubjectUseCase getSubjectUseCase,
            ListSubjectUseCase listSubjectUseCase,
            UpdateSubjectUseCase updateSubjectUseCase,
            DeleteSubjectUseCase deleteSubjectUseCase) {
        this.createSubjectUseCase = createSubjectUseCase;
        this.getSubjectUseCase = getSubjectUseCase;
        this.listSubjectUseCase = listSubjectUseCase;
        this.updateSubjectUseCase = updateSubjectUseCase;
        this.deleteSubjectUseCase = deleteSubjectUseCase;
    }

    public SubjectResponse create(SubjectCreateRequest request) {
        return createSubjectUseCase.execute(request);
    }

    public SubjectResponse getById(Long id) {
        return getSubjectUseCase.execute(id);
    }

    public Page<SubjectResponse> list(String keyword, Pageable pageable) {
        return listSubjectUseCase.search(keyword, pageable);
    }

    public SubjectResponse update(Long id, SubjectUpdateRequest request) {
        return updateSubjectUseCase.execute(id, request);
    }

    public void delete(Long id) {
        deleteSubjectUseCase.execute(id);
    }
}