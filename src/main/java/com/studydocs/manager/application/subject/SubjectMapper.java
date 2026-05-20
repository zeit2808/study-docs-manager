package com.studydocs.manager.application.subject;

import com.studydocs.manager.dto.subject.SubjectResponse;
import com.studydocs.manager.entity.Subject;

public final class SubjectMapper {
    private SubjectMapper() {}

    public static SubjectResponse toResponse(Subject s) {
        SubjectResponse r = new SubjectResponse();
        r.setName(s.getName());
        r.setSlug(s.getSlug());
        r.setDescription(s.getDescription());
        return r;
    }
}