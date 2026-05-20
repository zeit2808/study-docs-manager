package com.studydocs.manager.application.tag.usecase;

import com.studydocs.manager.application.tag.TagMapper;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.TagRepository;
import org.springframework.stereotype.Service;

@Service
public class GetTagUseCase {

    private final TagRepository tagRepository;

    public GetTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagResponse execute(Long id) {
        Tag tag = tagRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new NotFoundException(
                        "Tag not found",
                        "TAG_NOT_FOUND",
                        "id"));
        return TagMapper.toResponse(tag);
    }
}