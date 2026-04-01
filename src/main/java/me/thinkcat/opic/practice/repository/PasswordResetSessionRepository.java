package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.PasswordResetSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetSessionRepository extends JpaRepository<PasswordResetSession, UUID> {

    Optional<PasswordResetSession> findTopByUserIdAndBlockedUntilIsNotNullOrderByCreatedAtDesc(Long userId);
}
