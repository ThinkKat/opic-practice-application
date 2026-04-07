package me.thinkcat.opic.practice.dto.lambda;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LambdaFeedbackRequest {

    @JsonProperty("audioUrl")
    private final String audioUrl;

    @JsonProperty("userId")
    private final Long userId;

    @JsonProperty("questionText")
    private final String questionText;

    @JsonProperty("transcription")
    private final String transcription;
}
