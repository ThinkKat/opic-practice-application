package me.thinkcat.opic.practice.scheduler;

import me.thinkcat.opic.practice.AbstractIntegrationTest;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Transactional
class AnswerCleanupSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private AnswerCleanupScheduler scheduler;
    @Autowired private AnswerRepository answerRepository;
    @Autowired private DrillAnswerRepository drillAnswerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void SC_1_REQUESTED_TRANSCRIPTION_5분_초과_시_FAILED로_전환된다() {
        // given
        Answer answer = Answer.builder()
                .questionId(1L).sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();
        answer.requestFeedback();
        answer.setStatusChangedAt(LocalDateTime.now().minusMinutes(6));
        answerRepository.saveAndFlush(answer);

        // when
        scheduler.timeoutRequestedFeedbacks();

        // then
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM answers WHERE id = ?",
                String.class, answer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.FAILED.getCode());
    }

    @Test
    void SC_2_REQUESTED_FEEDBACK_5분_초과_시_FAILED로_전환된다() {
        // given
        Answer answer = Answer.builder()
                .questionId(1L).sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();
        answer.requestFeedbackProcessing();
        answer.setStatusChangedAt(LocalDateTime.now().minusMinutes(6));
        answerRepository.saveAndFlush(answer);

        // when
        scheduler.timeoutRequestedFeedbacks();

        // then
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM answers WHERE id = ?",
                String.class, answer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.FAILED.getCode());
    }

    @Test
    void SC_3_5분_미만이면_상태_변경_없음() {
        // given
        Answer answer = Answer.builder()
                .questionId(1L).sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();
        answer.requestFeedback();
        answer.setStatusChangedAt(LocalDateTime.now().minusMinutes(4));
        answerRepository.saveAndFlush(answer);

        // when
        scheduler.timeoutRequestedFeedbacks();

        // then
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM answers WHERE id = ?",
                String.class, answer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION.getCode());
    }

    @Test
    void SC_4_COMPLETED_5분_초과해도_변경_없음() {
        // given
        Answer answer = Answer.builder()
                .questionId(1L).sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();
        answer.requestFeedback();
        answer.completeFeedback();
        answer.setStatusChangedAt(LocalDateTime.now().minusMinutes(6));
        answerRepository.saveAndFlush(answer);

        // when
        scheduler.timeoutRequestedFeedbacks();

        // then
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM answers WHERE id = ?",
                String.class, answer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.COMPLETED.getCode());
    }

    @Test
    void I_1_콜백이_COMPLETED로_변경_후_스케줄러_실행시_FAILED로_덮어쓰지_않는다() {
        // given: 5분 초과된 REQUESTED_TRANSCRIPTION 상태 answer
        Answer answer = Answer.builder()
                .questionId(1L).sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();
        answer.requestFeedback();
        answer.setStatusChangedAt(LocalDateTime.now().minusMinutes(6));
        answerRepository.saveAndFlush(answer);

        // Lambda 콜백 도착: COMPLETED로 변경
        jdbcTemplate.update(
                "UPDATE answers SET feedback_status_code = ? WHERE id = ?",
                FeedbackStatus.COMPLETED.getCode(), answer.getId());

        // when: 스케줄러 실행 (벌크 UPDATE WHERE status IN (REQUESTED_TRANSCRIPTION, REQUESTED_FEEDBACK))
        scheduler.timeoutRequestedFeedbacks();

        // then: COMPLETED 상태 유지 (FAILED로 덮어쓰지 않음)
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM answers WHERE id = ?",
                String.class, answer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.COMPLETED.getCode());
    }

    @Test
    void drill_answer_REQUESTED_TRANSCRIPTION_5분_초과_시_FAILED로_전환된다() {
        // given
        DrillAnswer drillAnswer = DrillAnswer.builder()
                .userId(1L).questionId(1L)
                .audioUrl("uploads/drills/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3).mimeType("audio/m4a").durationMs(0)
                .build();
        drillAnswer.requestFeedback();
        drillAnswer.setStatusChangedAt(LocalDateTime.now().minusMinutes(6));
        drillAnswerRepository.saveAndFlush(drillAnswer);

        // when
        scheduler.timeoutRequestedFeedbacks();

        // then
        String status = jdbcTemplate.queryForObject(
                "SELECT feedback_status_code FROM drill_answers WHERE id = ?",
                String.class, drillAnswer.getId());
        Assertions.assertThat(status).isEqualTo(FeedbackStatus.FAILED.getCode());
    }
}
