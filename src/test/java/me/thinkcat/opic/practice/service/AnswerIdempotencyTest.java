package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.SessionStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.entity.UserRole;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * completeAnswerUpload와 handleS3UploadDetected가 동시에 실행됐을 때
 * Lambda invoke가 정확히 1회만 발생하는지 검증.
 *
 * 핵심 메커니즘:
 * - 두 메서드 모두 같은 Answer 엔티티 객체를 반환하도록 Mock 설정
 * - 먼저 실행된 메서드가 feedbackStatus를 REQUESTED로 변경
 * - 나중에 실행된 메서드는 isFeedbackNone() == false → Lambda 호출 없이 return
 */
@ExtendWith(MockitoExtension.class)
class AnswerIdempotencyTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private AnswerService answerService;

    private Answer pendingAnswer;
    private final Long answerId = 1L;
    private final Long userId = 10L;
    private final String fileKey = "uploads/sessions/1/questions/1/uuid.m4a";

    @BeforeEach
    void setUp() {
        pendingAnswer = Answer.builder()
                .questionId(1L)
                .sessionId(1L)
                .audioUrl(fileKey)
                .storageType(StorageType.S3)
                .mimeType("audio/m4a")
                .durationMs(0)
                .build();

        Session session = Session.builder()
                .id(1L).userId(userId).title("test").mode("EXAM")
                .statusCode(SessionStatus.IN_PROGRESS.getCode())
                .build();

        User paidUser = User.builder()
                .userRoleCode(UserRole.PAID.getCode())
                .build();

        // completeAnswerUpload 경로
        given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));

        // handleS3UploadDetected 경로
        given(answerRepository.findByAudioUrlForUpdate(fileKey)).willReturn(Optional.of(pendingAnswer));
        // completeUpload가 먼저 실행된 시나리오에서는 early return으로 도달하지 않으므로 lenient
        lenient().when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(paidUser));

        lenient().when(questionRepository.findById(any())).thenReturn(Optional.empty());
        given(answerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(presignedUrlService.generateDownloadUrl(anyString()))
                .willReturn(mock(PresignedUrlResponse.class));
    }

    @Test
    void completeUpload_선행_s3Detected_후행_Lambda_1회만_호출() {
        answerService.completeAnswerUpload(userId, UserRole.PAID, answerId, 5000);
        answerService.handleS3UploadDetected(fileKey);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED);
        verify(feedbackLambdaService, times(1))
                .invokeSessionFeedbackAsync(eq(fileKey), eq(userId), any());
    }

    @Test
    void s3Detected_선행_completeUpload_후행_Lambda_1회만_호출() {
        answerService.handleS3UploadDetected(fileKey);
        answerService.completeAnswerUpload(userId, UserRole.PAID, answerId, 5000);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED);
        verify(feedbackLambdaService, times(1))
                .invokeSessionFeedbackAsync(eq(fileKey), eq(userId), any());
    }

    @Test
    void FREE유저_두_경로_모두_Lambda_미호출() {
        given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().build())); // FREE (default)
        given(featureFlagService.isEnabled("ai-for-free")).willReturn(false);

        answerService.completeAnswerUpload(userId, UserRole.FREE, answerId, 5000);
        answerService.handleS3UploadDetected(fileKey);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.NONE);
        verify(feedbackLambdaService, never())
                .invokeSessionFeedbackAsync(any(), any(), any());
    }
}
