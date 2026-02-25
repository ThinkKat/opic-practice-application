package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.response.PrepareAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.SessionStatus;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AnswerServiceFileKeyTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private AnswerService answerService;

    private final Long userId = 10L;
    private final Long sessionId = 1L;
    private final Long questionId = 2L;

    @BeforeEach
    void setUp() {
        Session session = Session.builder()
                .id(sessionId).userId(userId).title("t").mode("EXAM")
                .statusCode(SessionStatus.IN_PROGRESS.getCode())
                .build();
        given(sessionRepository.findByIdAndUserId(sessionId, userId))
                .willReturn(Optional.of(session));

        given(presignedUrlService.generateUploadUrl(any()))
                .willReturn(PresignedUrlResponse.builder()
                        .uploadUrl("https://presigned.url")
                        .build());

        given(answerRepository.save(any())).willAnswer(inv -> {
            Answer a = inv.getArgument(0);
            return Answer.builder()
                    .id(100L)
                    .questionId(a.getQuestionId())
                    .sessionId(a.getSessionId())
                    .audioUrl(a.getAudioUrl())
                    .storageType(a.getStorageType())
                    .mimeType(a.getMimeType())
                    .durationMs(a.getDurationMs())
                    .build();
        });
    }

    @Test
    void fileKey가_올바른_S3_경로_패턴을_따른다() {
        PrepareAnswerUploadResponse response = answerService.prepareAnswerUpload(
                userId, sessionId, questionId, "recording.m4a", "audio/m4a", 1024L);

        assertThat(response.getFileKey())
                .matches("uploads/sessions/1/questions/2/[0-9a-f-]{36}\\.m4a");
    }

    @Test
    void 확장자_없는_파일명은_확장자_없이_키_생성() {
        PrepareAnswerUploadResponse response = answerService.prepareAnswerUpload(
                userId, sessionId, questionId, "recording", "audio/m4a", 1024L);

        assertThat(response.getFileKey())
                .matches("uploads/sessions/1/questions/2/[0-9a-f-]{36}");
    }
}
