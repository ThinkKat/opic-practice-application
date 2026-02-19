package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.DrillAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
