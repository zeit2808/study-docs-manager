package com.studydocs.manager.application.tag.usecase;

import com.studydocs.manager.application.tag.TagMapper;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ListTagUseCase {

    private final TagRepository tagRepository;

    public ListTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Page<TagResponse> execute(Pageable pageable) {
        return tagRepository.findByIsActiveTrue(pageable)
                .map(TagMapper::toResponse);
    }

    public Page<TagResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return execute(pageable);
        }
        return tagRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(keyword.trim(), pageable)
                .map(TagMapper::toResponse);
    }
}