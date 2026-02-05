package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class PrepareAnswerUploadResponse {

    private Long answerId;
    private String uploadUrl;
    private String fileKey;
    private LocalDateTime expiresAt;
    private Map<String, String> requiredHeaders;
}
