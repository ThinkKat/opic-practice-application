package me.thinkcat.opic.practice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;  // Presigned URL
    private String fileKey;    // S3 Key (uploads/{uuid}.mp3)
    private LocalDateTime expiresAt;
    private Map<String, String> requiredHeaders;  // Content-Type 등
}
