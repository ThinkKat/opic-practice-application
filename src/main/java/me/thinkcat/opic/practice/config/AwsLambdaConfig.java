package me.thinkcat.opic.practice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;

@Configuration
@ConfigurationProperties(prefix = "aws.lambda")
@Getter
@Setter
public class AwsLambdaConfig {

    private String sessionFeedbackFunctionName;
    private String drillAnswerFeedbackFunctionName;
    private String emailSenderFunctionName;

    @Bean
    public LambdaAsyncClient lambdaAsyncClient(PresignedUrlProperties s3Properties) {
        return LambdaAsyncClient.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
