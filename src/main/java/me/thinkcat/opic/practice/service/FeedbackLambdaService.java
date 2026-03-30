package me.thinkcat.opic.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.config.AwsLambdaConfig;
import me.thinkcat.opic.practice.config.PresignedUrlProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import me.thinkcat.opic.practice.dto.lambda.LambdaFeedbackRequest;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackLambdaService {

    private final LambdaAsyncClient lambdaAsyncClient;
    private final AwsLambdaConfig lambdaConfig;
    private final PresignedUrlProperties s3Properties;
    private final ObjectMapper objectMapper;
    private final LambdaFeedbackStatusUpdater lambdaFeedbackStatusUpdater;

    public void invokeSessionFeedbackAsync(String audioUrl, Long userId, String questionText) {
        invokeAsync(audioUrl, lambdaConfig.getSessionFeedbackFunctionName(), userId, questionText,
                () -> lambdaFeedbackStatusUpdater.failSessionFeedback(audioUrl));
    }

    public void invokeDrillAnswerFeedbackAsync(String audioUrl, Long userId, String questionText) {
        invokeAsync(audioUrl, lambdaConfig.getDrillAnswerFeedbackFunctionName(), userId, questionText,
                () -> lambdaFeedbackStatusUpdater.failDrillFeedback(audioUrl));
    }

    private void invokeAsync(String audioUrl, String lambdaFunctionName, Long userId, String questionText,
                             Runnable onInvokeFail) {
        LambdaFeedbackRequest requestDto = LambdaFeedbackRequest.builder()
                .source("WAS")
                .bucket(s3Properties.getBucket())
                .audioUrl(audioUrl)
                .userId(userId)
                .questionText(questionText)
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(requestDto);
        } catch (JsonProcessingException e) {
            log.error("event=feedback_lambda_payload_fail | audioUrl={} | error={}", audioUrl, e.getMessage());
            onInvokeFail.run();
            return;
        }

        InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaFunctionName)
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                .build();

        lambdaAsyncClient.invoke(request)
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("event=feedback_lambda_invoke_fail | audioUrl={} | userId={} | error={}", audioUrl, userId, ex.getMessage());
                        onInvokeFail.run();
                    } else {
                        log.info("event=feedback_lambda_invoke | audioUrl={} | userId={} | statusCode={}", audioUrl, userId, response.statusCode());
                    }
                });
    }
}
