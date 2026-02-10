package me.thinkcat.opic.practice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Where(clause = "deleted_at IS NULL")
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_set_id")
    private Long questionSetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mode;

    @Column(name = "status_code", nullable = false, columnDefinition = "char(7)")
    @JdbcTypeCode(Types.CHAR)
    private String statusCode;

    @Column(name = "current_index", nullable = false)
    @Builder.Default
    private Integer currentIndex = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Enum으로 상태 조회
    public SessionStatus getStatus() {
        return SessionStatus.fromCode(statusCode);
    }

    // 상태 전환 메서드
    public void start() {
        this.statusCode = SessionStatus.IN_PROGRESS.getCode();
        this.startedAt = LocalDateTime.now();
    }

    public void pause() {
        this.statusCode = SessionStatus.PAUSED.getCode();
    }

    public void resume() {
        this.statusCode = SessionStatus.IN_PROGRESS.getCode();
    }

    public void complete() {
        this.statusCode = SessionStatus.COMPLETED.getCode();
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.statusCode = SessionStatus.CANCELLED.getCode();
    }

    // 상태 확인 메서드
    public boolean isPending() {
        return getStatus() == SessionStatus.PENDING;
    }

    public boolean isInProgress() {
        return getStatus() == SessionStatus.IN_PROGRESS;
    }

    public boolean isPaused() {
        return getStatus() == SessionStatus.PAUSED;
    }

    public boolean isCompleted() {
        return getStatus() == SessionStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return getStatus() == SessionStatus.CANCELLED;
    }
}
