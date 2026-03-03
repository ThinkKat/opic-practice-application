package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadDetectedRequest {

    @NotBlank(message = "File key is required")
    @Pattern(regexp = "^uploads/.+", message = "File key must start with 'uploads/'")
    private String fileKey;
}
