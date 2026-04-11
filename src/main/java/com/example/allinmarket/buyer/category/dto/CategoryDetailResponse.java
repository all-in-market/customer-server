package com.example.allinmarket.buyer.category.dto;

import com.example.allinmarket.domain.category.entity.Category;

public record CategoryDetailResponse(
        Long id, String name, int sortOrder
) {
    public static CategoryDetailResponse from(Category category) {
        return new CategoryDetailResponse(
                category.getId(),
                category.getName(),
                category.getSortOrder()
        );
    }
}
