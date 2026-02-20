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
public class RecentDrillQuestionResponse {
    private String questionId;
    private String questionText;
    private String categoryName;
    private LocalDateTime lastPracticedAt;
    private long drillPracticeCount;
}
