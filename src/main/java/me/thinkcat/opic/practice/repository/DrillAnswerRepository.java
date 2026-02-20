package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.DrillAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrillAnswerRepository extends JpaRepository<DrillAnswer, Long> {

    Optional<DrillAnswer> findByAudioUrl(String audioUrl);

    List<DrillAnswer> findByUserIdAndQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
            Long userId, Long questionId, String uploadStatusCode);

    List<DrillAnswer> findByQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
            Long questionId, String uploadStatusCode);

    List<DrillAnswer> findByFeedbackStatusCodeAndUpdatedAtBefore(String feedbackStatusCode, LocalDateTime threshold);

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
}
