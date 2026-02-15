package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.entity.Answer;

public class AnswerMapper {

    public static AnswerResponse toResponse(Answer answer) {
        return AnswerResponse.builder()
                .id(answer.getId() != null ? answer.getId().toString() : null)
                .questionId(answer.getQuestionId() != null ? answer.getQuestionId().toString() : null)
                .sessionId(answer.getSessionId() != null ? answer.getSessionId().toString() : null)
                .audioUrl(answer.getAudioUrl())
                .storageType(answer.getStorageType())
                .mimeType(answer.getMimeType())
                .durationMs(answer.getDurationMs())
                .transcript(answer.getTranscript())
                .pauseAnalysis(answer.getPauseAnalysis())
                .feedback(answer.getFeedback())
                .uploadStatus(answer.getUploadStatus())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }
}
