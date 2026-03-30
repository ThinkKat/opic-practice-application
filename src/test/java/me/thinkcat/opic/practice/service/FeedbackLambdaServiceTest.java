package me.thinkcat.opic.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.thinkcat.opic.practice.config.AwsLambdaConfig;
import me.thinkcat.opic.practice.config.PresignedUrlProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackLambdaServiceTest {

    @Mock private LambdaAsyncClient lambdaAsyncClient;
    @Mock private AwsLambdaConfig lambdaConfig;
    @Mock private PresignedUrlProperties s3Properties;
    @Mock private LambdaFeedbackStatusUpdater lambdaFeedbackStatusUpdater;
    @Spy  private ObjectMapper objectMapper;
    @InjectMocks private FeedbackLambdaService feedbackLambdaService;

    private static final String AUDIO_URL = "uploads/sessions/1/questions/1/test.mp4";
    private static final Long USER_ID = 10L;
    private static final String QUESTION_TEXT = "Tell me about yourself.";
    private static final String FUNCTION_NAME = "audio-transcriber";

    @Test
    void invokeSessionFeedbackAsync_invoke실패시_failSessionFeedback이_호출된다() {
        // given
        given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        given(s3Properties.getBucket()).willReturn("test-bucket");
        given(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Lambda connection error")));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater).failSessionFeedback(AUDIO_URL);
        verify(lambdaFeedbackStatusUpdater, never()).failDrillFeedback(any());
    }

    @Test
    void invokeSessionFeedbackAsync_invoke성공시_failSessionFeedback이_호출되지_않는다() {
        // given
        given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        given(s3Properties.getBucket()).willReturn("test-bucket");
        given(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater, never()).failSessionFeedback(any());
        verify(lambdaFeedbackStatusUpdater, never()).failDrillFeedback(any());
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_invoke실패시_failDrillFeedback이_호출된다() {
        // given
        given(lambdaConfig.getDrillAnswerFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        given(s3Properties.getBucket()).willReturn("test-bucket");
        given(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Lambda connection error")));

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater).failDrillFeedback(AUDIO_URL);
        verify(lambdaFeedbackStatusUpdater, never()).failSessionFeedback(any());
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_invoke성공시_failDrillFeedback이_호출되지_않는다() {
        // given
        given(lambdaConfig.getDrillAnswerFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        given(s3Properties.getBucket()).willReturn("test-bucket");
        given(lambdaAsyncClient.invoke(any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater, never()).failDrillFeedback(any());
        verify(lambdaFeedbackStatusUpdater, never()).failSessionFeedback(any());
    }

    @Test
    void invokeSessionFeedbackAsync_payload직렬화실패시_failSessionFeedback이_호출된다() throws Exception {
        // given
        given(s3Properties.getBucket()).willReturn("test-bucket");
        doThrow(new JsonProcessingException("serialize error") {})
                .when(objectMapper).writeValueAsString(any());

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater).failSessionFeedback(AUDIO_URL);
        verify(lambdaAsyncClient, never()).invoke(any(InvokeRequest.class));
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_payload직렬화실패시_failDrillFeedback이_호출된다() throws Exception {
        // given
        given(s3Properties.getBucket()).willReturn("test-bucket");
        doThrow(new JsonProcessingException("serialize error") {})
                .when(objectMapper).writeValueAsString(any());

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(AUDIO_URL, USER_ID, QUESTION_TEXT);

        // then
        verify(lambdaFeedbackStatusUpdater).failDrillFeedback(AUDIO_URL);
        verify(lambdaAsyncClient, never()).invoke(any(InvokeRequest.class));
    }
}
