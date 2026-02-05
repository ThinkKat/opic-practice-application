package me.thinkcat.opic.practice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 유령 데이터 정리 Scheduler
 *
 * 30분 이상 PENDING 상태로 방치된 Answer 레코드를 FAILED로 변경합니다.
 *
 * ⚠️ 현재 비활성화 상태입니다.
 * 활성화하려면 아래 @Scheduled 어노테이션의 주석을 해제하고,
 * @EnableScheduling을 Application 클래스에 추가하세요.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnswerCleanupScheduler {

    private final AnswerRepository answerRepository;

    /**
     * 매일 새벽 3시에 실행 (비활성화 상태)
     * 30분 이상 PENDING 상태인 레코드를 FAILED로 변경
     *
     * 활성화 방법:
     * 1. 아래 @Scheduled 어노테이션 주석 해제
     * 2. OpicPracticeApplication 클래스에 @EnableScheduling 추가
     */
    // @Scheduled(cron = "0 0 3 * * *")  // 비활성화: 주석 처리됨
    @Transactional
    public void cleanupOrphanedAnswers() {
        log.info("Starting orphaned answers cleanup task...");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Answer> orphanedAnswers = answerRepository
                .findByUploadStatusAndUpdatedAtBefore(UploadStatus.PENDING, threshold);

        if (orphanedAnswers.isEmpty()) {
            log.info("No orphaned answers found.");
            return;
        }

        orphanedAnswers.forEach(answer -> {
            answer.setUploadStatus(UploadStatus.FAILED);
            log.warn("Marked answer {} as FAILED due to timeout (created: {}, sessionId: {})",
                    answer.getId(), answer.getCreatedAt(), answer.getSessionId());
        });

        answerRepository.saveAll(orphanedAnswers);

        log.info("Cleaned up {} orphaned answers", orphanedAnswers.size());
    }

    /**
     * 수동 실행용 메서드 (테스트/관리용)
     * 필요 시 REST API 엔드포인트를 통해 호출 가능
     */
    public void cleanupNow() {
        log.info("Manual cleanup triggered");
        cleanupOrphanedAnswers();
    }
}
