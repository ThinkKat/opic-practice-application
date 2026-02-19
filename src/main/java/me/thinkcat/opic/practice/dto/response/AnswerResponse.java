package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.thinkcat.opic.practice.entity.StorageType;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private String id;
    private String questionId;
    private String sessionId;
    private String audioUrl;
    private StorageType storageType;
    private String mimeType;
    private Integer durationMs;
    private String transcript;
    private String pauseAnalysis;
    private String feedback;
    private String uploadStatus;
    private String uploadStatusText;
    private String feedbackStatus;
    private String feedbackStatusText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
