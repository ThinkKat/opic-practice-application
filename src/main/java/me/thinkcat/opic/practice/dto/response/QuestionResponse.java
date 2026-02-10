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
public class QuestionResponse {
    private String id;
    private String categoryId;
    private String questionTypeId;
    private String question;
    private String audioFileUrl;
    private Integer durationMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
