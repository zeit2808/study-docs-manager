package com.studydocs.manager.service.common;

import java.util.function.Predicate;

public interface SlugService {
    String slugify(String input);
    String uniqueSlug(String input, Predicate<String> existsSlug);
}
