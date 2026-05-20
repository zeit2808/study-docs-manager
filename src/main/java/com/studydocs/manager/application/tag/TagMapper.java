package com.studydocs.manager.application.tag;

import com.studydocs.manager.dto.tag.TagResponse;
import com.studydocs.manager.entity.Tag;

public final class TagMapper {
    private TagMapper() {}

    public static TagResponse toResponse(Tag t) {
        TagResponse r = new TagResponse();
        r.setId(t.getId());
        r.setName(t.getName());
        r.setSlug(t.getSlug());
        return r;
    }
}