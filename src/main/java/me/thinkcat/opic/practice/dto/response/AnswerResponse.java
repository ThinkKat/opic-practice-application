package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {
    private Long id;
    private Long questionId;
    private Long sessionId;
    private String audioUri;
    private String mimeType;
    private Integer durationMs;
    private String transcript;
    private LocalDateTime createdAt;
}
