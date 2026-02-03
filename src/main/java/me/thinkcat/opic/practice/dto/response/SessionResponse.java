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
public class SessionResponse {
    private Long id;
    private Long userId;
    private Long questionSetId;
    private String title;
    private String mode;
    private String status;
    private String statusText;
    private Integer currentIndex;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
