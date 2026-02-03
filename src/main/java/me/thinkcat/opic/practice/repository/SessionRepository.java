package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserId(Long userId);

    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Session> findByUserIdAndStatusCode(Long userId, String statusCode);

    Optional<Session> findByIdAndUserId(Long id, Long userId);
}
