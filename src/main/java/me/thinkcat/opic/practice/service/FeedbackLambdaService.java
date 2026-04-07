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
import me.thinkcat.opic.practice.dto.lambda.LambdaInvokePayload;
import me.thinkcat.opic.practice.dto.lambda.LambdaInvokePayload;

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

    public void invokeSessionFeedbackAsync(LambdaFeedbackRequest dto) {
        invokeAsync(dto, lambdaConfig.getSessionFeedbackFunctionName(),
                () -> lambdaFeedbackStatusUpdater.failSessionFeedback(dto.getAudioUrl()));
    }

    public void invokeDrillAnswerFeedbackAsync(LambdaFeedbackRequest dto) {
        invokeAsync(dto, lambdaConfig.getDrillAnswerFeedbackFunctionName(),
                () -> lambdaFeedbackStatusUpdater.failDrillFeedback(dto.getAudioUrl()));
    }

    private void invokeAsync(LambdaFeedbackRequest requestDto, String lambdaFunctionName, Runnable onInvokeFail) {
        LambdaInvokePayload invokePayload = LambdaInvokePayload.builder()
                .source("WAS")
                .bucket(s3Properties.getBucket())
                .request(requestDto)
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(invokePayload);
        } catch (JsonProcessingException e) {
            log.error("event=feedback_lambda_payload_fail | audioUrl={} | error={}", requestDto.getAudioUrl(), e.getMessage());
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
                        log.error("event=feedback_lambda_invoke_fail | audioUrl={} | userId={} | error={}", requestDto.getAudioUrl(), requestDto.getUserId(), ex.getMessage());
                        onInvokeFail.run();
                    } else {
                        log.info("event=feedback_lambda_invoke | audioUrl={} | userId={} | statusCode={}", requestDto.getAudioUrl(), requestDto.getUserId(), response.statusCode());
                    }
                });
    }
}
