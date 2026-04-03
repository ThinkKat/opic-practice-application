package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.lambda.LambdaFeedbackRequest;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.entity.UserRole;
import me.thinkcat.opic.practice.repository.CategoryRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DrillAnswerServiceTest {

    @Mock private DrillAnswerRepository drillAnswerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private DrillAnswerService drillAnswerService;

    private DrillAnswer pendingAnswer;
    private final Long drillAnswerId = 1L;
    private final Long userId = 10L;

    @BeforeEach
    void setUp() {
        pendingAnswer = DrillAnswer.builder()
                .userId(userId)
                .questionId(1L)
                .audioUrl("uploads/drills/10/questions/1/uuid.m4a")
                .storageType(StorageType.S3)
                .mimeType("audio/m4a")
                .durationMs(0)
                .build();
    }

    @Test
    void 첫_피드백_요청_시_transcript_없으면_transcription이_null로_invoke된다() {
        // given
        User paidUser = User.builder().userRoleCode(UserRole.PAID.getCode()).build();
        BDDMockito.given(drillAnswerRepository.findByIdForUpdate(drillAnswerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(questionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());
        BDDMockito.given(drillAnswerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));
        BDDMockito.given(presignedUrlService.generateDownloadUrl(ArgumentMatchers.anyString())).willReturn(Mockito.mock(me.thinkcat.opic.practice.dto.response.PresignedUrlResponse.class));

        // when
        drillAnswerService.submitDrillAnswer(userId, UserRole.PAID, drillAnswerId, 5000);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeDrillAnswerFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isNull();
    }

    @Test
    void retryFeedback_transcript_있으면_transcription_포함해서_invoke된다() {
        // given
        pendingAnswer.failFeedback();
        pendingAnswer.setTranscript("Hello, my name is John.");
        BDDMockito.given(drillAnswerRepository.findByIdForUpdate(drillAnswerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(questionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());
        BDDMockito.given(drillAnswerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        // when
        drillAnswerService.retryFeedback(drillAnswerId, userId);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeDrillAnswerFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isEqualTo("Hello, my name is John.");
    }

    @Test
    void retryFeedback_transcript_없으면_transcription_null로_invoke된다() {
        // given
        pendingAnswer.failFeedback();
        BDDMockito.given(drillAnswerRepository.findByIdForUpdate(drillAnswerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(questionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());
        BDDMockito.given(drillAnswerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        // when
        drillAnswerService.retryFeedback(drillAnswerId, userId);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeDrillAnswerFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isNull();
    }

    @Test
    void updateTranscription_호출_후_feedbackStatus가_REQUESTED_FEEDBACK으로_변환된다() {
        // given
        BDDMockito.given(drillAnswerRepository.findByAudioUrl(pendingAnswer.getAudioUrl()))
                .willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(drillAnswerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        // when
        drillAnswerService.updateTranscription(pendingAnswer.getAudioUrl(), "transcript", null, null, null);

        // then
        Assertions.assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_FEEDBACK);
    }

    @Test
    void retryFeedback_FAILED_아닌_상태에서_호출하면_ValidationException_발생() {
        // given
        pendingAnswer.completeFeedback();
        BDDMockito.given(drillAnswerRepository.findByIdForUpdate(drillAnswerId)).willReturn(Optional.of(pendingAnswer));

        // when & then
        Assertions.assertThatThrownBy(() -> drillAnswerService.retryFeedback(drillAnswerId, userId))
                .isInstanceOf(me.thinkcat.opic.practice.exception.ValidationException.class);
    }
}
