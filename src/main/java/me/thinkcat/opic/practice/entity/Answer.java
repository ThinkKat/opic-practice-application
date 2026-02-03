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

    @Column(name = "audio_uri", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "mime_type", nullable = false, columnDefinition = "TEXT")
    private String mimeType;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(columnDefinition = "TEXT")
    private String transcript;
}
