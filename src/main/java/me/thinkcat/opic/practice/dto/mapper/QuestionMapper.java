package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.entity.Question;

public class QuestionMapper {

    public static QuestionResponse toResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .categoryId(question.getCategoryId())
                .questionTypeId(question.getQuestionTypeId())
                .question(question.getQuestion())
                .audioFileUri(question.getAudioFileUri())
                .durationMs(question.getDurationMs())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
