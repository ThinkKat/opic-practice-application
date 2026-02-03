package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다")
    @Size(max = 255, message = "파일명은 255자를 초과할 수 없습니다")
    private String fileName;

    @NotBlank(message = "Content-Type은 필수입니다")
    @Pattern(regexp = "^audio/(mpeg|wav|m4a|webm|mp4)$",
             message = "허용되지 않은 파일 형식입니다")
    private String contentType;

    @NotNull(message = "파일 크기는 필수입니다")
    @Min(value = 1, message = "파일 크기는 1바이트 이상이어야 합니다")
    @Max(value = 52428800, message = "파일 크기는 50MB를 초과할 수 없습니다")
    private Long contentLength;
}
