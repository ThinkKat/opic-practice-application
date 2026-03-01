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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackLambdaService {

    private final LambdaAsyncClient lambdaAsyncClient;
    private final AwsLambdaConfig lambdaConfig;
    private final PresignedUrlProperties s3Properties;
    private final ObjectMapper objectMapper;

    public void invokeSessionFeedbackAsync(String audioUrl, Long userId, String questionText) {
        invokeAsync(audioUrl, lambdaConfig.getSessionFeedbackFunctionName(), userId, questionText);
    }

    public void invokeDrillAnswerFeedbackAsync(String audioUrl, Long userId, String questionText) {
        invokeAsync(audioUrl, lambdaConfig.getDrillAnswerFeedbackFunctionName(), userId, questionText);
    }

    private void invokeAsync(String audioUrl, String lambdaFunctionName, Long userId, String questionText) {
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("source", "WAS");
        payloadMap.put("bucket", s3Properties.getBucket());
        payloadMap.put("audioUrl", audioUrl);
        payloadMap.put("userId", userId);
        payloadMap.put("questionText", questionText);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            log.error("event=feedback_lambda_payload_fail | audioUrl={} | error={}", audioUrl, e.getMessage());
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
                    } else {
                        log.info("event=feedback_lambda_invoke | audioUrl={} | userId={} | statusCode={}", audioUrl, userId, response.statusCode());
                    }
                });
    }
}
