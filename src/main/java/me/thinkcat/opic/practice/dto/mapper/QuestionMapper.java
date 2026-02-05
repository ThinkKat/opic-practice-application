package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.entity.Question;

public class QuestionMapper {

    public static QuestionResponse toResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId() != null ? question.getId().toString() : null)
                .categoryId(question.getCategoryId() != null ? question.getCategoryId().toString() : null)
                .questionTypeId(question.getQuestionTypeId() != null ? question.getQuestionTypeId().toString() : null)
                .question(question.getQuestion())
                .audioFileUrl(question.getAudioFileUrl())
                .durationMs(question.getDurationMs())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
