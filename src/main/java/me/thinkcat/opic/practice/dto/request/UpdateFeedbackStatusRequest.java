package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.thinkcat.opic.practice.entity.FeedbackFailureReason;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeedbackStatusRequest {

    @NotBlank(message = "Audio URL must not be blank")
    private String audioUrl;

    @NotNull(message = "Reason must not be null")
    private FeedbackFailureReason reason;
}
