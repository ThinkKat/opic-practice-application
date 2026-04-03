package me.thinkcat.opic.practice.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AnswerTest {

    private Answer answer;

    @BeforeEach
    void setup() {
        answer = new Answer();
    }

    @Test
    void 람다_요청시_feedbackStatus_FBS0002로_변환() {
        // when
        answer.requestFeedback();

        // then
        Assertions.assertThat(answer.getFeedbackStatusCode()).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION.getCode());
        Assertions.assertThat(answer.getStatusChangedAt()).isNotNull();
    }

    @Test
    void 전사_완료시_feedbackStatus_FBS0006로_변환() {
        // when
        answer.requestFeedbackProcessing();

        // then
        Assertions.assertThat(answer.getFeedbackStatusCode()).isEqualTo(FeedbackStatus.REQUESTED_FEEDBACK.getCode());
        Assertions.assertThat(answer.getStatusChangedAt()).isNotNull();
    }

    @Test
    void 피드백_완료시_feedbackStatusCode_FBS0003로_변환() {
        // when
        answer.completeFeedback();

        // then
        Assertions.assertThat(answer.getFeedbackStatusCode()).isEqualTo(FeedbackStatus.COMPLETED.getCode());
        Assertions.assertThat(answer.getStatusChangedAt()).isNotNull();
    }

    @Test
    void 피드백_실패시_feedbackStatusCode_FBS0004로_변환() {
        // when
        answer.failFeedback();

        // then
        Assertions.assertThat(answer.getFeedbackStatusCode()).isEqualTo(FeedbackStatus.FAILED.getCode());
        Assertions.assertThat(answer.getStatusChangedAt()).isNotNull();
    }

    @Test
    void REQUESTED_TRANSCRIPTION_상태에서_isFeedbackRequested가_true() {
        // given
        answer.requestFeedback();

        // when & then
        Assertions.assertThat(answer.isFeedbackRequested()).isTrue();
    }

    @Test
    void REQUESTED_FEEDBACK_상태에서_isFeedbackRequested가_true() {
        // given
        answer.requestFeedbackProcessing();

        // when & then
        Assertions.assertThat(answer.isFeedbackRequested()).isTrue();
    }

    @Test
    void COMPLETED_상태에서_isFeedbackRequested가_false() {
        // given
        answer.completeFeedback();

        // when & then
        Assertions.assertThat(answer.isFeedbackRequested()).isFalse();
    }
}
