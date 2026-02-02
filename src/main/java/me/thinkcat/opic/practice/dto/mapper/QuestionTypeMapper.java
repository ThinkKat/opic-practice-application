package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.QuestionTypeResponse;
import me.thinkcat.opic.practice.entity.QuestionType;

public class QuestionTypeMapper {

    public static QuestionTypeResponse toResponse(QuestionType questionType) {
        return QuestionTypeResponse.builder()
                .id(questionType.getId())
                .name(questionType.getName())
                .createdAt(questionType.getCreatedAt())
                .updatedAt(questionType.getUpdatedAt())
                .build();
    }
}
