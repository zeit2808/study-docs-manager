package com.studydocs.manager.application.tag.usecase;

import com.studydocs.manager.application.tag.TagMapper;
import com.studydocs.manager.dto.tag.TagCreateRequest;
import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.entity.Tag;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.repository.TagRepository;
import com.studydocs.manager.service.common.SlugService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CreateTagUseCase {

    private final TagRepository tagRepository;
    private final SlugService slugService;

    public CreateTagUseCase(TagRepository tagRepository, SlugService slugService) {
        this.tagRepository = tagRepository;
        this.slugService = slugService;
    }

    @Transactional
    public TagResponse execute(TagCreateRequest request) {
        String normalizedName = normalizeName(request.getName());

        Optional<Tag> existingOptional = tagRepository.findByNameIgnoreCase(normalizedName);
        if (existingOptional.isPresent()) {
            Tag existing = existingOptional.get();
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new ConflictException("Tag name already exists", "TAG_NAME_EXISTS", "name");
            }

            existing.setActive(true);
            existing.setName(normalizedName);
            if (existing.getSlug() == null || existing.getSlug().trim().isEmpty()) {
                existing.setSlug(slugService.uniqueSlug(normalizedName, slug -> {
                    if (slug.equals(existing.getSlug())) {
                        return false;
                    }
                    return tagRepository.existsBySlug(slug);
                }));
            }
            Tag reactivated = tagRepository.save(existing);
            return TagMapper.toResponse(reactivated);
        }

        Tag tag = new Tag();
        tag.setName(normalizedName);
        tag.setSlug(slugService.uniqueSlug(normalizedName, tagRepository::existsBySlug));
        tag.setActive(true);

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