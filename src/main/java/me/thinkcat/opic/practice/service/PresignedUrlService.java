package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.config.FileStorageProperties;
import me.thinkcat.opic.practice.config.PresignedUrlProperties;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.exception.PresignedUrlException;
import me.thinkcat.opic.practice.exception.ValidationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresignedUrlService {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final PresignedUrlProperties properties;
    private final FileStorageProperties fileStorageProperties;

    /**
     * 업로드용 Presigned URL 생성
     *
     * @param request fileKey, contentType, contentLength 포함
     * @return Presigned URL 정보
     */
    public PresignedUrlResponse generateUploadUrl(PresignedUrlRequest request) {
        validateFileRequest(request);
        validateFileKey(request.getFileKey());

        try {
            // PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(request.getFileKey())
                    .contentType(request.getContentType())
                    .contentLength(request.getContentLength())
                    .build();

            // Presigned Request 생성
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(properties.getPresignedUrlExpiration()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Presigned URL 생성
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            log.info("Generated upload URL for key: {}", request.getFileKey());

            return PresignedUrlResponse.builder()
                    .uploadUrl(presignedRequest.url().toString())
                    .fileKey(request.getFileKey())
                    .expiresAt(LocalDateTime.now().plusSeconds(properties.getPresignedUrlExpiration()))
                    .requiredHeaders(Map.of(
                            "Content-Type", request.getContentType(),
                            "Content-Length", String.valueOf(request.getContentLength())
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate upload URL for key: {}", request.getFileKey(), e);
            throw new PresignedUrlException("Failed to generate upload URL", e);
        }
    }

    /**
     * S3 파일 키 생성 유틸리티 (공개 메서드)
     * 형식: uploads/{uuid}.{extension}
     *
     * @param originalFileName 원본 파일명
     * @return 생성된 S3 파일 키
     */
    public String generateSimpleFileKey(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        return "uploads/" + uniqueId + extension;
    }

    /**
     * 다운로드용 Presigned URL 생성
     */
    public PresignedUrlResponse generateDownloadUrl(String fileKey) {
        validateFileKey(fileKey);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(properties.getPresignedUrlExpiration()))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            log.info("Generated download URL for key: {}", fileKey);

            return PresignedUrlResponse.builder()
                    .uploadUrl(presignedRequest.url().toString())
                    .fileKey(fileKey)
                    .expiresAt(LocalDateTime.now().plusSeconds(properties.getPresignedUrlExpiration()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate download URL for file: {}", fileKey, e);
            throw new PresignedUrlException("Failed to generate download URL", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void validateFileRequest(PresignedUrlRequest request) {
        // Content-Type 검증
        if (!fileStorageProperties.getAllowedTypes().contains(request.getContentType())) {
            throw new ValidationException(
                    "Unsupported content type: " + request.getContentType());
        }

        // 파일 크기 검증
        long maxSize = parseSize(fileStorageProperties.getMaxFileSize());
        if (request.getContentLength() > maxSize) {
            throw new ValidationException(
                    "File size exceeds maximum: " + fileStorageProperties.getMaxFileSize());
        }
    }

    private void validateFileKey(String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            throw new ValidationException("File key cannot be empty");
        }

        if (!fileKey.startsWith("uploads/")) {
            throw new ValidationException("Invalid file key: " + fileKey);
        }

        // 경로 조작 시도 차단
        if (fileKey.contains("..") || fileKey.contains("//")) {
            throw new ValidationException("Invalid file key: path traversal attempt detected");
        }
    }

    private long parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return 50 * 1024 * 1024; // 기본 50MB
        }
        String upperSize = size.toUpperCase();
        if (upperSize.endsWith("MB")) {
            return Long.parseLong(upperSize.replace("MB", "").trim()) * 1024 * 1024;
        }
        if (upperSize.endsWith("KB")) {
            return Long.parseLong(upperSize.replace("KB", "").trim()) * 1024;
        }
        return Long.parseLong(size);
    }

    /**
     * S3 파일 존재 여부 확인
     *
     * @param fileKey S3 파일 키
     * @return 파일 존재 여부
     */
    public boolean checkFileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(fileKey)
                    .build();

            s3Client.headObject(headRequest);
            log.debug("File exists in S3: {}", fileKey);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("File does not exist in S3: {}", fileKey);
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in S3: {}", fileKey, e);
            return false;
        }
    }
}
