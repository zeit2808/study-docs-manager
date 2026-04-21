package com.studydocs.manager.enums;

/** Tiêu chí sắp xếp kết quả tìm kiếm document. */
public enum SortOption {
    RELEVANCE, // Sort by search score
    DATE, // Sort by createdAt
    UPDATED, // Sort by updatedAt
    RATING, // Sort by ratingAverage
    FAVORITES, // Sort by favouriteCount
    TITLE // Sort by title alphabetically
}
