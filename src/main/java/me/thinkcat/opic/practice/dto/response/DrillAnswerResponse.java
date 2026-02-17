package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrillAnswerResponse {
    private String id;
    private String userId;
    private String questionId;
    private String audioUrl;
    private StorageType storageType;
    private String mimeType;
    private Integer durationMs;
    private String transcript;
    private String pauseAnalysis;
    private String feedback;
    private UploadStatus uploadStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
