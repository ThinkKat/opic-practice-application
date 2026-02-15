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
    @Column(name = "word_segments", columnDefinition = "jsonb")
    private String wordSegments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pause_analysis", columnDefinition = "jsonb")
    private String pauseAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback", columnDefinition = "jsonb")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 10)
    private UploadStatus uploadStatus;
}
