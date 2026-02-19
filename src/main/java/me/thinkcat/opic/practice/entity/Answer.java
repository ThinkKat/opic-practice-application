package me.thinkcat.opic.practice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.type.SqlTypes;

import java.sql.Types;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 10)
    private StorageType storageType;

    @Column(name = "mime_type", nullable = false, columnDefinition = "TEXT")
    private String mimeType;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_segments")
    private String wordSegments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pause_analysis")
    private String pauseAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback")
    private String feedback;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "upload_status_code", nullable = false, columnDefinition = "char(7)")
    @Builder.Default
    private String uploadStatusCode = UploadStatus.PENDING.getCode();

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "feedback_status_code", nullable = false, columnDefinition = "char(7)")
    @Builder.Default
    private String feedbackStatusCode = FeedbackStatus.NONE.getCode();

    public UploadStatus getUploadStatus() {
        return UploadStatus.fromCode(uploadStatusCode);
    }

    public void markUploadSuccess() {
        this.uploadStatusCode = UploadStatus.SUCCESS.getCode();
    }

    public void markUploadFailed() {
        this.uploadStatusCode = UploadStatus.FAILED.getCode();
    }

    public boolean isUploadPending() {
        return getUploadStatus() == UploadStatus.PENDING;
    }

    public boolean isUploadSuccess() {
        return getUploadStatus() == UploadStatus.SUCCESS;
    }

    public boolean isUploadFailed() {
        return getUploadStatus() == UploadStatus.FAILED;
    }

    public FeedbackStatus getFeedbackStatus() {
        return FeedbackStatus.fromCode(feedbackStatusCode);
    }

    public void requestFeedback() {
        this.feedbackStatusCode = FeedbackStatus.REQUESTED.getCode();
    }

    public void completeFeedback() {
        this.feedbackStatusCode = FeedbackStatus.COMPLETED.getCode();
    }

    public void failFeedback() {
        this.feedbackStatusCode = FeedbackStatus.FAILED.getCode();
    }

    public void invalidateFeedback() {
        this.feedbackStatusCode = FeedbackStatus.INVALID.getCode();
    }

    public boolean isFeedbackNone() {
        return getFeedbackStatus() == FeedbackStatus.NONE;
    }

    public boolean isFeedbackRequested() {
        return getFeedbackStatus() == FeedbackStatus.REQUESTED;
    }

    public boolean isFeedbackCompleted() {
        return getFeedbackStatus() == FeedbackStatus.COMPLETED;
    }

    public boolean isFeedbackFailed() {
        return getFeedbackStatus() == FeedbackStatus.FAILED;
    }

    public boolean isFeedbackInvalid() {
        return getFeedbackStatus() == FeedbackStatus.INVALID;
    }
}
