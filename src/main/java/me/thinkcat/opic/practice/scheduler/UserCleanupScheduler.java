package me.thinkcat.opic.practice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴 계정 하드 삭제 Scheduler
 * soft delete 후 30일 경과한 계정을 매일 새벽 4시에 일괄 삭제
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCleanupScheduler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${scheduler.refresh-token-cleanup.retention-days}")
    private int refreshTokenRetentionDays;

    /**
     * 탈퇴 유저 30일 이후에 실제 정보 물리적 삭제.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void hardDeleteWithdrawnUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<User> targets = userRepository.findSoftDeletedBefore(threshold);

        if (targets.isEmpty()) {
            return;
        }

        for (User user : targets) {
            refreshTokenRepository.deleteByUserId(user.getId());
            userRepository.delete(user);
            log.info("event=scheduler_user_deleted | userId={} | deletedAt={}", user.getId(), user.getDeletedAt());
        }

        log.info("event=scheduler_user_cleanup_done | count={}", targets.size());
    }

    /**
     * 90일 지난 refresh token 삭제
     */
    @Scheduled(cron = "${scheduler.refresh-token-cleanup.cron}")
    @Transactional
    public void cleanupOldRefreshTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(refreshTokenRetentionDays);
        refreshTokenRepository.deleteOlderThan(cutoff);
        log.info("event=scheduler_token_cleanup_done | cutoff={}", cutoff);
    }
}
