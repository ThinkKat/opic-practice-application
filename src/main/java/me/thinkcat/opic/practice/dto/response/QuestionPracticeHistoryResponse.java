package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPracticeHistoryResponse {

    private QuestionResponse question;
    private CategoryResponse category;
    private QuestionTypeResponse questionType;
    private List<SessionAnswerInfo> sessionAnswers;
    private List<DrillAnswerResponse> drillAnswers;
    private PracticeStatistics statistics;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionAnswerInfo {
        private AnswerResponse answer;
        private String sessionId;
        private String sessionTitle;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PracticeStatistics {
        private long totalPracticeCount;
        private long sessionPracticeCount;
        private long drillPracticeCount;
    }
}
