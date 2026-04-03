package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.lambda.LambdaFeedbackRequest;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.*;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.BDDMockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import org.assertj.core.api.Assertions;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock private AnswerRepository answerRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private FeatureFlagService featureFlagService;
    @Mock private FeedbackLambdaService feedbackLambdaService;
    @Mock private PresignedUrlService presignedUrlService;
    @InjectMocks private AnswerService answerService;

    private Answer pendingAnswer;
    private Session session;
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
                .uploadStatusCode(UploadStatus.SUCCESS.getCode())
                .build();

        session = Session.builder()
                .id(1L).userId(userId).title("test").mode("EXAM")
                .statusCode(SessionStatus.IN_PROGRESS.getCode())
                .build();

    }

    @Test
    void FREE유저_aiForFree_플래그ON_피드백요청되고_Lambda_1회호출() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(featureFlagService.isEnabled("ai-for-free")).willReturn(true);
        BDDMockito.given(presignedUrlService.generateDownloadUrl(ArgumentMatchers.anyString())).willReturn(Mockito.mock(PresignedUrlResponse.class));
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        BDDMockito.given(questionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());
        BDDMockito.given(answerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        answerService.completeAnswerUpload(userId, UserRole.FREE, answerId, 5000);

        Assertions.assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION);
        Mockito.verify(feedbackLambdaService, Mockito.times(1))
                .invokeSessionFeedbackAsync(ArgumentMatchers.any(LambdaFeedbackRequest.class));
    }

    @Test
    void FREE유저_aiForFree_플래그OFF_피드백없고_Lambda_미호출() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(featureFlagService.isEnabled("ai-for-free")).willReturn(false);
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        BDDMockito.given(presignedUrlService.generateDownloadUrl(ArgumentMatchers.anyString())).willReturn(Mockito.mock(PresignedUrlResponse.class));
        BDDMockito.given(answerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        answerService.completeAnswerUpload(userId, UserRole.FREE, answerId, 5000);

        Assertions.assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.NONE);
        Mockito.verify(feedbackLambdaService, Mockito.never()).invokeSessionFeedbackAsync(ArgumentMatchers.any(LambdaFeedbackRequest.class));
    }

    @Test
    void PAID유저_플래그무관하게_피드백요청되고_Lambda_1회호출() {
        //given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        BDDMockito.given(presignedUrlService.generateDownloadUrl(ArgumentMatchers.anyString())).willReturn(Mockito.mock(PresignedUrlResponse.class));
        BDDMockito.given(questionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());
        BDDMockito.given(answerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        // when
        answerService.completeAnswerUpload(userId, UserRole.PAID, answerId, 5000);

        Assertions.assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_TRANSCRIPTION);
        Mockito.verify(feedbackLambdaService, Mockito.times(1))
                .invokeSessionFeedbackAsync(ArgumentMatchers.any(LambdaFeedbackRequest.class));
    }

    @Test
    void 첫_피드백_요청_시_transcript_없으면_transcription이_null로_invoke된다() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        BDDMockito.given(presignedUrlService.generateDownloadUrl(ArgumentMatchers.anyString())).willReturn(Mockito.mock(PresignedUrlResponse.class));
        BDDMockito.given(answerRepository.save(ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        // when
        answerService.completeAnswerUpload(userId, UserRole.PAID, answerId, 5000);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeSessionFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isNull();
    }

    @Test
    void retryFeedback_transcript_있으면_transcription_포함해서_invoke된다() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        pendingAnswer.failFeedback();
        pendingAnswer.setTranscript("Hello, my name is John.");

        // when
        answerService.retryFeedback(answerId, userId);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeSessionFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isEqualTo("Hello, my name is John.");
    }

    @Test
    void retryFeedback_transcript_없으면_transcription_null로_invoke된다() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        BDDMockito.given(sessionRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(session));
        pendingAnswer.failFeedback();

        // when
        answerService.retryFeedback(answerId, userId);

        // then
        ArgumentCaptor<LambdaFeedbackRequest> captor = ArgumentCaptor.forClass(LambdaFeedbackRequest.class);
        Mockito.verify(feedbackLambdaService).invokeSessionFeedbackAsync(captor.capture());
        Assertions.assertThat(captor.getValue().getTranscription()).isNull();
    }

    @Test
    void updateTranscription_호출_후_feedbackStatus가_REQUESTED_FEEDBACK으로_변환된다() {
        // given
        BDDMockito.given(answerRepository.findByAudioUrl(pendingAnswer.getAudioUrl())).willReturn(Optional.of(pendingAnswer));

        // when
        answerService.updateTranscription(pendingAnswer.getAudioUrl(), "transcript", null, null, null);

        // then
        Assertions.assertThat(pendingAnswer.getFeedbackStatus()).isEqualTo(FeedbackStatus.REQUESTED_FEEDBACK);
    }

    @Test
    void retryFeedback_FAILED_아닌_상태에서_호출하면_ValidationException_발생() {
        // given
        BDDMockito.given(answerRepository.findByIdForUpdate(answerId)).willReturn(Optional.of(pendingAnswer));
        pendingAnswer.completeFeedback();

        // when & then
        Assertions.assertThatThrownBy(() -> answerService.retryFeedback(answerId, userId))
                .isInstanceOf(me.thinkcat.opic.practice.exception.ValidationException.class);
    }
}
