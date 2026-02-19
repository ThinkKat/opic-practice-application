package me.thinkcat.opic.practice.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackLambdaService {

    private final LambdaAsyncClient lambdaAsyncClient;
    private final AwsLambdaConfig lambdaConfig;
    private final PresignedUrlProperties s3Properties;

    public void invokeAsync(String audioUrl) {
        String payload = String.format(
                "{\"source\":\"WAS\",\"bucket\":\"%s\",\"audioUrl\":\"%s\"}",
                s3Properties.getBucket(),
                audioUrl
        );

        InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaConfig.getFeedbackFunctionName())
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                .build();

        lambdaAsyncClient.invoke(request)
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("Failed to invoke feedback Lambda for audioUrl={}: {}", audioUrl, ex.getMessage());
                    } else {
                        log.info("Feedback Lambda invoked for audioUrl={}, statusCode={}", audioUrl, response.statusCode());
                    }
                });
    }
}
