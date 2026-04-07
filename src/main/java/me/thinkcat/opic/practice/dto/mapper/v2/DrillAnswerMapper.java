package me.thinkcat.opic.practice.dto.mapper.v2;

import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.UploadStatus;

public class DrillAnswerMapper {

    public static DrillAnswerResponse toResponse(DrillAnswer answer, String resolvedAudioUrl) {
        UploadStatus uploadStatus = answer.getUploadStatus();
        FeedbackStatus feedbackStatus = answer.getFeedbackStatus();

        return DrillAnswerResponse.builder()
                .id(answer.getId() != null ? answer.getId().toString() : null)
                .userId(answer.getUserId() != null ? answer.getUserId().toString() : null)
                .questionId(answer.getQuestionId() != null ? answer.getQuestionId().toString() : null)
                .audioUrl(resolvedAudioUrl)
                .storageType(answer.getStorageType())
                .mimeType(answer.getMimeType())
                .durationMs(answer.getDurationMs())
                .transcript(answer.getTranscript())
                .pauseAnalysis(answer.getPauseAnalysis())
                .feedback(answer.getFeedback())
                .uploadStatus(uploadStatus.name())
                .uploadStatusText(uploadStatus.getText())
                .feedbackStatus(feedbackStatus.name())
                .feedbackStatusText(feedbackStatus.getText())
                .statusChangedAt(answer.getStatusChangedAt())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }
}
