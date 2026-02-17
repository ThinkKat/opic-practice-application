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
public class UpdateDrillTranscriptionRequest {

    @NotBlank(message = "Audio URL is required")
    private String audioUrl;

    @NotBlank(message = "Transcription is required")
    private String transcription;

    private String wordSegments;

    private String pauseAnalysis;

    private Double duration;
}
