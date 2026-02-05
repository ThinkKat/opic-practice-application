package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.UploadStatus;
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

    /**
     * 유령 데이터 정리용: 특정 상태이고 특정 시간 이전에 업데이트된 레코드 조회
     *
     * @param uploadStatus 업로드 상태
     * @param threshold    기준 시간
     * @return 조건에 해당하는 Answer 목록
     */
    List<Answer> findByUploadStatusAndUpdatedAtBefore(UploadStatus uploadStatus, LocalDateTime threshold);
}
