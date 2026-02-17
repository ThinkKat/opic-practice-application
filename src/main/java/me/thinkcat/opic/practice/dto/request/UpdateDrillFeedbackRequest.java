package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDrillFeedbackRequest {

    @NotBlank(message = "Audio URL is required")
    private String audioUrl;

    @NotBlank(message = "Feedback is required")
    private String feedback;
}
