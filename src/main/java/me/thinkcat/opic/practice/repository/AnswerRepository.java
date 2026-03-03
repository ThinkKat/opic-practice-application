package me.thinkcat.opic.practice.repository;

import jakarta.persistence.LockModeType;
import me.thinkcat.opic.practice.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Answer a WHERE a.id = :id")
    Optional<Answer> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Answer a WHERE a.audioUrl = :audioUrl")
    Optional<Answer> findByAudioUrlForUpdate(@Param("audioUrl") String audioUrl);

    List<Answer> findByUploadStatusCodeAndUpdatedAtBefore(String uploadStatusCode, LocalDateTime threshold);

    List<Answer> findByFeedbackStatusCodeAndUpdatedAtBefore(String feedbackStatusCode, LocalDateTime threshold);

    List<Answer> findByQuestionId(Long questionId);

    @Query("""
            SELECT a.questionId AS questionId, COUNT(a) AS count
            FROM Answer a
            JOIN Session s ON a.sessionId = s.id
            WHERE a.questionId IN :questionIds
              AND s.userId = :userId
              AND a.uploadStatusCode = :uploadStatusCode
            GROUP BY a.questionId
            """)
    List<QuestionPracticeCountProjection> countByQuestionIdsAndUserId(
            @Param("questionIds") List<Long> questionIds,
            @Param("userId") Long userId,
            @Param("uploadStatusCode") String uploadStatusCode);
}
