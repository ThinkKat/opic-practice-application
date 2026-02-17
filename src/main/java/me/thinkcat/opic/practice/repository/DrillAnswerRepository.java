package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrillAnswerRepository extends JpaRepository<DrillAnswer, Long> {

    Optional<DrillAnswer> findByAudioUrl(String audioUrl);

    List<DrillAnswer> findByUserIdAndQuestionIdAndUploadStatusOrderByCreatedAtDesc(
            Long userId, Long questionId, UploadStatus uploadStatus);

    List<DrillAnswer> findByQuestionIdAndUploadStatusOrderByCreatedAtDesc(
            Long questionId, UploadStatus uploadStatus);
}
