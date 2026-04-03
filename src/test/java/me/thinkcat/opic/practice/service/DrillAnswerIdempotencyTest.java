package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.lambda.LambdaFeedbackRequest;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.entity.UserRole;
import me.thinkcat.opic.practice.repository.CategoryRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
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
 * submitDrillAnswer와 handleS3UploadDetected가 동시에 실행됐을 때
 * Lambda invoke가 정확히 1회만 발생하는지 검증.
 */
@ExtendWith(MockitoExtension.class)
class DrillAnswerIdempotencyTest {

    @Mock private DrillAnswerRepository drillAnswerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private DrillAnswerService drillAnswerService;

    private DrillAnswer pendingAnswer;
    private final Long drillAnswerId = 1L;
    private final Long userId = 10L;
    private final String fileKey = "uploads/drills/10/questions/1/uuid.m4a";

    @BeforeEach
    void setUp() {
        pendingAnswer = DrillAnswer.builder()
                .userId(userId)
                .questionId(1L)
                .audioUrl(fileKey)
                .storageType(StorageType.S3)
                .mimeType("audio/m4a")
                .durationMs(0)
                .build();

        User paidUser = User.builder()
                .userRoleCode(UserRole.PAID.getCode())
                .build();

        // submitDrillAnswer 경로
        given(drillAnswerRepository.findByIdForUpdate(drillAnswerId)).willReturn(Optional.of(pendingAnswer));

        // handleS3UploadDetected 경로
        given(drillAnswerRepository.findByAudioUrlForUpdate(fileKey)).willReturn(Optional.of(pendingAnswer));
        // submit이 먼저 실행된 시나리오에서는 early return으로 도달하지 않으므로 lenient
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(paidUser));

        lenient().when(questionRepository.findById(any())).thenReturn(Optional.empty());
        given(drillAnswerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(presignedUrlService.generateDownloadUrl(anyString()))
                .willReturn(mock(PresignedUrlResponse.class));
    }

    @Test
    void submit_선행_s3Detected_후행_Lambda_1회만_호출() {
        drillAnswerService.submitDrillAnswer(userId, UserRole.PAID, drillAnswerId, 5000);
        drillAnswerService.handleS3UploadDetected(fileKey);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION);
        verify(feedbackLambdaService, times(1))
                .invokeDrillAnswerFeedbackAsync(any(LambdaFeedbackRequest.class));
    }

    @Test
    void s3Detected_선행_submit_후행_Lambda_1회만_호출() {
        drillAnswerService.handleS3UploadDetected(fileKey);
        drillAnswerService.submitDrillAnswer(userId, UserRole.PAID, drillAnswerId, 5000);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION);
        verify(feedbackLambdaService, times(1))
                .invokeDrillAnswerFeedbackAsync(any(LambdaFeedbackRequest.class));
    }

    @Test
    void FREE유저_두_경로_모두_Lambda_미호출() {
        given(userRepository.findById(userId)).willReturn(Optional.of(User.builder().build())); // FREE (default)
        given(featureFlagService.isEnabled("ai-for-free")).willReturn(false);

        drillAnswerService.submitDrillAnswer(userId, UserRole.FREE, drillAnswerId, 5000);
        drillAnswerService.handleS3UploadDetected(fileKey);

        assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.NONE);
        verify(feedbackLambdaService, never())
                .invokeDrillAnswerFeedbackAsync(any(LambdaFeedbackRequest.class));
    }
}
