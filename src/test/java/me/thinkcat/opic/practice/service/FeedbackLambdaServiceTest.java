package me.thinkcat.opic.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.thinkcat.opic.practice.config.AwsLambdaConfig;
import me.thinkcat.opic.practice.config.PresignedUrlProperties;
import me.thinkcat.opic.practice.dto.lambda.LambdaFeedbackRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.concurrent.CompletableFuture;

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
        BDDMockito.given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Lambda connection error")));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater).failSessionFeedback(AUDIO_URL);
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failDrillFeedback(ArgumentMatchers.any());
    }

    @Test
    void invokeSessionFeedbackAsync_invoke성공시_failSessionFeedback이_호출되지_않는다() {
        // given
        BDDMockito.given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failSessionFeedback(ArgumentMatchers.any());
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failDrillFeedback(ArgumentMatchers.any());
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_invoke실패시_failDrillFeedback이_호출된다() {
        // given
        BDDMockito.given(lambdaConfig.getDrillAnswerFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("Lambda connection error")));

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater).failDrillFeedback(AUDIO_URL);
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failSessionFeedback(ArgumentMatchers.any());
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_invoke성공시_failDrillFeedback이_호출되지_않는다() {
        // given
        BDDMockito.given(lambdaConfig.getDrillAnswerFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failDrillFeedback(ArgumentMatchers.any());
        Mockito.verify(lambdaFeedbackStatusUpdater, Mockito.never()).failSessionFeedback(ArgumentMatchers.any());
    }

    @Test
    void invoke_시_payload에_source_bucket_request_포함된다() {
        // given
        BDDMockito.given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        Mockito.verify(lambdaAsyncClient).invoke(captor.capture());
        String payload = captor.getValue().payload().asUtf8String();
        Assertions.assertThat(payload).contains("\"source\"");
        Assertions.assertThat(payload).contains("\"bucket\"");
        Assertions.assertThat(payload).contains("\"request\"");
    }

    @Test
    void transcription_null인_DTO_invoke_시_payload에_transcription이_null로_포함된다() {
        // given
        BDDMockito.given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        Mockito.verify(lambdaAsyncClient).invoke(captor.capture());
        String payload = captor.getValue().payload().asUtf8String();
        Assertions.assertThat(payload).contains("\"transcription\":null");
    }

    @Test
    void transcription_값_있는_DTO_invoke_시_payload에_transcription_값이_포함된다() {
        // given
        BDDMockito.given(lambdaConfig.getSessionFeedbackFunctionName()).willReturn(FUNCTION_NAME);
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        BDDMockito.given(lambdaAsyncClient.invoke(ArgumentMatchers.any(InvokeRequest.class)))
                .willReturn(CompletableFuture.completedFuture(InvokeResponse.builder().statusCode(202).build()));

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT)
                .transcription("Hello, my name is John.")
                .build());

        // then
        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        Mockito.verify(lambdaAsyncClient).invoke(captor.capture());
        String payload = captor.getValue().payload().asUtf8String();
        Assertions.assertThat(payload).contains("\"transcription\":\"Hello, my name is John.\"");
    }

    @Test
    void invokeSessionFeedbackAsync_payload직렬화실패시_failSessionFeedback이_호출된다() throws Exception {
        // given
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        Mockito.doThrow(new JsonProcessingException("serialize error") {})
                .when(objectMapper).writeValueAsString(ArgumentMatchers.any());

        // when
        feedbackLambdaService.invokeSessionFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater).failSessionFeedback(AUDIO_URL);
        Mockito.verify(lambdaAsyncClient, Mockito.never()).invoke(ArgumentMatchers.any(InvokeRequest.class));
    }

    @Test
    void invokeDrillAnswerFeedbackAsync_payload직렬화실패시_failDrillFeedback이_호출된다() throws Exception {
        // given
        BDDMockito.given(s3Properties.getBucket()).willReturn("test-bucket");
        Mockito.doThrow(new JsonProcessingException("serialize error") {})
                .when(objectMapper).writeValueAsString(ArgumentMatchers.any());

        // when
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(LambdaFeedbackRequest.builder()
                .audioUrl(AUDIO_URL).userId(USER_ID).questionText(QUESTION_TEXT).build());

        // then
        Mockito.verify(lambdaFeedbackStatusUpdater).failDrillFeedback(AUDIO_URL);
        Mockito.verify(lambdaAsyncClient, Mockito.never()).invoke(ArgumentMatchers.any(InvokeRequest.class));
    }
}
