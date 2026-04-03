package me.thinkcat.opic.practice.dto.lambda;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LambdaInvokePayload {

    @JsonProperty("source")
    private final String source;

    @JsonProperty("bucket")
    private final String bucket;

    @JsonProperty("request")
    private final LambdaFeedbackRequest request;
}
