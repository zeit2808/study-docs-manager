package com.studydocs.manager.application.tag.usecase;

import com.studydocs.manager.application.tag.TagMapper;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.dto.tag.TagUpdateRequest;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.TagRepository;
import com.studydocs.manager.service.common.SlugService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UpdateTagUseCase {

    private final TagRepository tagRepository;
    private final SlugService slugService;

    public UpdateTagUseCase(TagRepository tagRepository, SlugService slugService) {
        this.tagRepository = tagRepository;
        this.slugService = slugService;
    }

    @Transactional
    public TagResponse execute(Long id, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Tag not found",
                        "TAG_NOT_FOUND",
                        "id"));

        if (request.getName() != null) {
            String normalizedName = normalizeName(request.getName());
            boolean nameChanged = !normalizedName.equalsIgnoreCase(tag.getName());

            if (nameChanged) {
                Optional<Tag> existing = tagRepository.findByNameIgnoreCase(normalizedName);
                if (existing.isPresent() && !existing.get().getId().equals(tag.getId()) && Boolean.TRUE.equals(existing.get().getIsActive())) {
                    throw new ConflictException("Tag name already exists", "TAG_NAME_EXISTS", "name");
                }

                tag.setName(normalizedName);
                tag.setSlug(slugService.uniqueSlug(normalizedName, slug -> {
                    if (slug.equals(tag.getSlug())) {
                        return false;
                    }
                    return tagRepository.existsBySlug(slug);
                }));
            }
        }

        if (request.getIsActive() != null) {
            tag.setIsActive(request.getIsActive());
        }

        Tag saved = tagRepository.save(tag);
        return TagMapper.toResponse(saved);
    }

    private String normalizeName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Tag name must not be blank", "INVALID_TAG_NAME", "name");
        }
        return value.trim();
    }
}