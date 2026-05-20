package com.studydocs.manager.application.tag.usecase;

import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.TagRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DeleteTagUseCase {

    private final TagRepository tagRepository;

    public DeleteTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void execute(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Tag not found",
                        "TAG_NOT_FOUND",
                        "id"));

        if (Boolean.FALSE.equals(tag.getIsActive())) {
            return;
        }

        tag.setIsActive(false);
        tagRepository.save(tag);
    }
}