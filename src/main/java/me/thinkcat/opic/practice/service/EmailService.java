package me.thinkcat.opic.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.config.AwsLambdaConfig;
import me.thinkcat.opic.practice.dto.lambda.LambdaEmailRequest;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final LambdaAsyncClient lambdaAsyncClient;
    private final AwsLambdaConfig lambdaConfig;
    private final ObjectMapper objectMapper;

    public void sendPasswordResetCode(String to, String code, int validMinutes) {
        LambdaEmailRequest requestDto = LambdaEmailRequest.builder()
                .to(to)
                .subject("[OPIc Practice] 비밀번호 재설정 코드")
                .body("비밀번호 재설정 코드: " + code + "\n\n코드는 " + validMinutes + "분간 유효합니다.")
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(requestDto);
        } catch (JsonProcessingException e) {
            log.error("event=password_reset_email_payload_fail | to={} | error={}", to, e.getMessage());
            return;
        }


        InvokeRequest request = InvokeRequest.builder()
                .functionName(lambdaConfig.getEmailSenderFunctionName())
                .invocationType(InvocationType.EVENT)
                .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                .build();

        lambdaAsyncClient.invoke(request)
                .whenComplete((response, ex) -> {
                    if (ex != null) {
                        log.error("event=password_reset_email_invoke_fail | to={} | error={}", to, ex.getMessage());
                    } else {
                        log.info("event=password_reset_email_sent | to={} | statusCode={}", to, response.statusCode());
                    }
                });
    }
}
