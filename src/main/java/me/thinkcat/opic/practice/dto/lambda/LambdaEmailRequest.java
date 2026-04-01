package me.thinkcat.opic.practice.dto.lambda;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LambdaEmailRequest {
    private String to;
    private String subject;
    private String body;
}
