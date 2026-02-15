package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeedbackRequest {

    @NotBlank(message = "Audio URL must not be blank")
    private String audioUrl;

    @NotBlank(message = "Feedback must not be blank")
    private String feedback;
}
