package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.CategoryResponse;
import me.thinkcat.opic.practice.entity.Category;

public class CategoryMapper {

    public static CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
