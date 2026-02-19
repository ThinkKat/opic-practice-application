package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findBySessionId(Long sessionId);

    List<Answer> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    Optional<Answer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    Optional<Answer> findByAudioUrl(String audioUrl);

    List<Answer> findByUploadStatusCodeAndUpdatedAtBefore(String uploadStatusCode, LocalDateTime threshold);

    List<Answer> findByFeedbackStatusCodeAndUpdatedAtBefore(String feedbackStatusCode, LocalDateTime threshold);

    List<Answer> findByQuestionId(Long questionId);
}
