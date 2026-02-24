package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.SessionStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UserRole;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private AnswerService answerService;

    private Answer pendingAnswer;
    private final Long answerId = 1L;
    private final Long userId = 10L;

    @BeforeEach
    void setUp() {
        pendingAnswer = Answer.builder()
                .questionId(1L)
                .sessionId(1L)
                .audioUrl("uploads/sessions/1/questions/1/uuid.m4a")
                .storageType(StorageType.S3)
                .mimeType("audio/m4a")
                .durationMs(0)
                .build();

        Session session = Session.builder()
                .id(1L).userId(userId).title("test").mode("EXAM")
                .statusCode(SessionStatus.IN_PROGRESS.getCode())
                .build();

        given(answerRepository.findById(answerId)).willReturn(Optional.of(pendingAnswer));
        given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        given(answerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(presignedUrlService.generateDownloadUrl(anyString()))
                .willReturn(mock(PresignedUrlResponse.class));
    }

    @Test
    void FREE유저_aiForFree_플래그ON_피드백요청되고_Lambda_1회호출() {
        given(featureFlagService.isEnabled("ai-for-free")).willReturn(true);

        answerService.completeAnswerUpload(userId, UserRole.FREE, answerId, 5000);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED);
        verify(feedbackLambdaService, times(1))
                .invokeSessionFeedbackAsync(pendingAnswer.getAudioUrl());
    }

    @Test
    void FREE유저_aiForFree_플래그OFF_피드백없고_Lambda_미호출() {
        given(featureFlagService.isEnabled("ai-for-free")).willReturn(false);

        answerService.completeAnswerUpload(userId, UserRole.FREE, answerId, 5000);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.NONE);
        verify(feedbackLambdaService, never()).invokeSessionFeedbackAsync(any());
    }

    @Test
    void PAID유저_플래그무관하게_피드백요청되고_Lambda_1회호출() {
        answerService.completeAnswerUpload(userId, UserRole.PAID, answerId, 5000);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED);
        verify(feedbackLambdaService, times(1))
                .invokeSessionFeedbackAsync(pendingAnswer.getAudioUrl());
    }
}
