package com.studydocs.manager.application.tag;

import com.studydocs.manager.application.tag.usecase.CreateTagUseCase;
import com.studydocs.manager.application.tag.usecase.DeleteTagUseCase;
import com.studydocs.manager.application.tag.usecase.GetTagUseCase;
import com.studydocs.manager.application.tag.usecase.ListTagUseCase;
import com.studydocs.manager.application.tag.usecase.UpdateTagUseCase;
import com.studydocs.manager.dto.tag.TagCreateRequest;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.dto.tag.TagUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TagApplicationService {

    private final CreateTagUseCase createTagUseCase;
    private final GetTagUseCase getTagUseCase;
    private final ListTagUseCase listTagUseCase;
    private final UpdateTagUseCase updateTagUseCase;
    private final DeleteTagUseCase deleteTagUseCase;

    public TagApplicationService(
            CreateTagUseCase createTagUseCase,
            GetTagUseCase getTagUseCase,
            ListTagUseCase listTagUseCase,
            UpdateTagUseCase updateTagUseCase,
            DeleteTagUseCase deleteTagUseCase) {
        this.createTagUseCase = createTagUseCase;
        this.getTagUseCase = getTagUseCase;
        this.listTagUseCase = listTagUseCase;
        this.updateTagUseCase = updateTagUseCase;
        this.deleteTagUseCase = deleteTagUseCase;
    }

    public TagResponse create(TagCreateRequest request) {
        return createTagUseCase.execute(request);
    }

    public TagResponse getById(Long id) {
        return getTagUseCase.execute(id);
    }

    public Page<TagResponse> list(String keyword, Pageable pageable) {
        return listTagUseCase.search(keyword, pageable);
    }

    public TagResponse update(Long id, TagUpdateRequest request) {
        return updateTagUseCase.execute(id, request);
    }

    public void delete(Long id) {
        deleteTagUseCase.execute(id);
    }
}