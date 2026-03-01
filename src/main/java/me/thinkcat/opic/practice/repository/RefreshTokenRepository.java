package me.thinkcat.opic.practice.repository;

import jakarta.persistence.LockModeType;
import me.thinkcat.opic.practice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.token = :token")
    Optional<RefreshToken> findByTokenWithLock(@Param("token") String token);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            DELETE FROM RefreshToken r WHERE
            (r.revoked = true  AND r.revokedAt  < :cutoff)
            OR
            (r.revoked = false AND r.expiresAt  < :cutoff)
            """)
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
