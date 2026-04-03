package me.thinkcat.opic.practice.repository;

import jakarta.persistence.LockModeType;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrillAnswerRepository extends JpaRepository<DrillAnswer, Long> {

    Optional<DrillAnswer> findByAudioUrl(String audioUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DrillAnswer d WHERE d.id = :id")
    Optional<DrillAnswer> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DrillAnswer d WHERE d.audioUrl = :audioUrl")
    Optional<DrillAnswer> findByAudioUrlForUpdate(@Param("audioUrl") String audioUrl);

    List<DrillAnswer> findByUserIdAndQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
            Long userId, Long questionId, String uploadStatusCode);

    List<DrillAnswer> findByQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
            Long questionId, String uploadStatusCode);

    List<DrillAnswer> findByFeedbackStatusCodeInAndStatusChangedAtBefore(List<String> feedbackStatusCodes, LocalDateTime threshold);

    @Query("""
            SELECT d.questionId AS questionId,
                   MAX(d.createdAt) AS lastPracticedAt,
                   COUNT(d.id) AS drillPracticeCount
            FROM DrillAnswer d
            WHERE d.userId = :userId
              AND d.uploadStatusCode = :uploadStatusCode
            GROUP BY d.questionId
            ORDER BY MAX(d.createdAt) DESC
            """)
    List<RecentDrillQuestionProjection> findRecentlyPracticedQuestions(
            @Param("userId") Long userId,
            @Param("uploadStatusCode") String uploadStatusCode);

    @Query("""
            SELECT d.questionId AS questionId, COUNT(d) AS count
            FROM DrillAnswer d
            WHERE d.questionId IN :questionIds
              AND d.userId = :userId
              AND d.uploadStatusCode = :uploadStatusCode
            GROUP BY d.questionId
            """)
    List<QuestionPracticeCountProjection> countByQuestionIdsAndUserId(
            @Param("questionIds") List<Long> questionIds,
            @Param("userId") Long userId,
            @Param("uploadStatusCode") String uploadStatusCode);
}
