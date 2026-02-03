package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.entity.Answer;

public class AnswerMapper {

    public static AnswerResponse toResponse(Answer answer) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .sessionId(answer.getSessionId())
                .audioUrl(answer.getAudioUrl())
                .mimeType(answer.getMimeType())
                .durationMs(answer.getDurationMs())
                .transcript(answer.getTranscript())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
