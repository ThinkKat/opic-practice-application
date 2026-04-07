package me.thinkcat.opic.practice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유령 데이터 정리 Scheduler
 *
 * ⚠️ 현재 비활성화 상태입니다.
 * 활성화하려면:
 * 1. 아래 @Scheduled 어노테이션의 주석을 해제
 * 2. OpicPracticeApplication 클래스에 @EnableScheduling 추가
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerCleanupScheduler {

    private final AnswerRepository answerRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 매일 새벽 3시에 실행 (비활성화 상태)
     * 30분 이상 PENDING 상태인 레코드를 FAILED로 변경
     */
    // @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanedAnswers() {
        log.info("event=scheduler_answer_cleanup_start");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Answer> orphanedAnswers = answerRepository
                .findByUploadStatusCodeAndUpdatedAtBefore(UploadStatus.PENDING.getCode(), threshold);

        if (orphanedAnswers.isEmpty()) {
            log.info("No orphaned answers found.");
            return;
        }

        orphanedAnswers.forEach(answer -> {
            answer.markUploadFailed();
            log.warn("event=scheduler_answer_timeout | answerId={} | sessionId={} | createdAt={}",
                    answer.getId(), answer.getSessionId(), answer.getCreatedAt());
        });

        answerRepository.saveAll(orphanedAnswers);

        log.info("Cleaned up {} orphaned answers", orphanedAnswers.size());
    }

    /**
     * 5분마다 실행 (비활성화 상태)
     * statusChangedAt 기준 5분 이상 REQUESTED_TRANSCRIPTION 또는 REQUESTED_FEEDBACK 상태로 방치된 피드백을 FAILED로 변경
     */
    // @Scheduled(fixedDelay = 300000)
    public void timeoutRequestedFeedbacks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        LocalDateTime now = LocalDateTime.now();
        String failedCode = FeedbackStatus.FAILED.getCode();
        String transcriptionCode = FeedbackStatus.REQUESTED_TRANSCRIPTION.getCode();
        String feedbackCode = FeedbackStatus.REQUESTED_FEEDBACK.getCode();

        String sql = "UPDATE %s SET feedback_status_code = ?, status_changed_at = ? " +
                     "WHERE feedback_status_code IN (?, ?) AND status_changed_at < ?";

        int answersUpdated = jdbcTemplate.update(String.format(sql, "answers"),
                failedCode, now, transcriptionCode, feedbackCode, threshold);
        int drillAnswersUpdated = jdbcTemplate.update(String.format(sql, "drill_answers"),
                failedCode, now, transcriptionCode, feedbackCode, threshold);

        if (answersUpdated > 0) {
            log.warn("event=scheduler_feedback_timeout | table=answers | count={}", answersUpdated);
        }
        if (drillAnswersUpdated > 0) {
            log.warn("event=scheduler_feedback_timeout | table=drill_answers | count={}", drillAnswersUpdated);
        }
    }
}
