package me.thinkcat.opic.practice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Getter
@Setter
public class PresignedUrlProperties {
    private boolean enabled = false;
    private String bucket;
    private String region;
    private int presignedUrlExpiration = 900; // 15분 (초 단위)
}
