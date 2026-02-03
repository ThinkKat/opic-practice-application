package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompleteAnswerUploadRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotBlank(message = "File key is required")
    @Pattern(regexp = "^uploads/.+", message = "File key must start with 'uploads/'")
    private String fileKey;

    @NotBlank(message = "MIME type is required")
    @Pattern(regexp = "^audio/(mpeg|wav|m4a|webm|mp4)$",
             message = "Invalid audio MIME type")
    private String mimeType;

    @Min(value = 0, message = "Duration must be non-negative")
    private Integer durationMs;
}
