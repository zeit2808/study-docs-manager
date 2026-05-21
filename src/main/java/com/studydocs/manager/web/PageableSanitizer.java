package com.studydocs.manager.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PageableSanitizer {

    private PageableSanitizer() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedProperties, Sort fallbackSort) {
        if (pageable == null) {
            return PageRequest.of(0, 20, fallbackSort);
        }

        List<Sort.Order> validOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (property == null) {
                continue;
            }

            String normalized = property.trim();
            if (allowedProperties.contains(normalized)) {
                validOrders.add(order.withProperty(normalized));
            }
        }

        Sort safeSort = validOrders.isEmpty() ? fallbackSort : Sort.by(validOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }
}
