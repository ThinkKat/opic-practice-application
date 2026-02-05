package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepareAnswerUploadRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;

    private Long contentLength;
}
