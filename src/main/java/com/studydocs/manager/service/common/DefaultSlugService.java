package com.studydocs.manager.service.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class DefaultSlugService implements SlugService {
    @Override
    public String slugify(String input) {
        String base = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        base = base.replace('đ', 'd');
        base = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        base = base.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return base.isEmpty() ? "n-a" : base;
    }

    @Override
    public String uniqueSlug(String input, Predicate<String> existsSlug) {
        String base = slugify(input);
        String candidate = base;
        int i = 1;
        while (existsSlug.test(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

}
