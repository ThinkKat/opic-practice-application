package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.config.FileStorageProperties;
import me.thinkcat.opic.practice.config.PresignedUrlProperties;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URL;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresignedUrlService 테스트")
class PresignedUrlServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedUrlProperties presignedUrlProperties;

    @Mock
    private FileStorageProperties fileStorageProperties;

    @InjectMocks
    private PresignedUrlService presignedUrlService;

    @BeforeEach
    void setUp() {
        // Properties 설정
        when(presignedUrlProperties.getBucket()).thenReturn("test-bucket");
        when(presignedUrlProperties.getRegion()).thenReturn("ap-northeast-2");
        when(presignedUrlProperties.getPresignedUrlExpiration()).thenReturn(900);

        when(fileStorageProperties.getMaxFileSize()).thenReturn("50MB");
        when(fileStorageProperties.getAllowedTypes()).thenReturn(
                List.of("audio/mpeg", "audio/wav", "audio/m4a", "audio/webm", "audio/mp4")
        );
    }

    @Test
    @DisplayName("정상적인 업로드 URL 생성")
    void generateUploadUrl_Success() throws Exception {
        // Given
        PresignedUrlRequest request = new PresignedUrlRequest(
                "test-audio.mp3",
                "audio/mpeg",
                1024L * 1024L // 1MB
        );

        URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/uploads/test.mp3");
        PresignedPutObjectRequest mockPresignedRequest = PresignedPutObjectRequest.builder()
                .url(mockUrl)
                .expiration(Instant.now().plusSeconds(900))
                .build();

        when(s3Presigner.presignPutObject(any())).thenReturn(mockPresignedRequest);

        // When
        PresignedUrlResponse response = presignedUrlService.generateUploadUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUploadUrl()).isNotNull();
        assertThat(response.getFileKey()).startsWith("uploads/");
        assertThat(response.getFileKey()).endsWith(".mp3");
        assertThat(response.getExpiresAt()).isNotNull();
        assertThat(response.getRequiredHeaders()).containsKey("Content-Type");
        assertThat(response.getRequiredHeaders()).containsKey("Content-Length");
    }

    @Test
    @DisplayName("파일 크기 초과 시 실패")
    void generateUploadUrl_FileSizeExceeds_Failure() {
        // Given: 100MB 파일
        PresignedUrlRequest request = new PresignedUrlRequest(
                "large-file.mp3",
                "audio/mpeg",
                100L * 1024L * 1024L // 100MB
        );

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateUploadUrl(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File size exceeds maximum");
    }

    @Test
    @DisplayName("잘못된 Content-Type으로 실패")
    void generateUploadUrl_InvalidContentType_Failure() {
        // Given
        PresignedUrlRequest request = new PresignedUrlRequest(
                "test.txt",
                "text/plain",
                1024L
        );

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateUploadUrl(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Unsupported content type");
    }

    @Test
    @DisplayName("정상적인 다운로드 URL 생성")
    void generateDownloadUrl_Success() throws Exception {
        // Given
        String fileKey = "uploads/test-123.mp3";

        URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/uploads/test-123.mp3");
        PresignedGetObjectRequest mockPresignedRequest = PresignedGetObjectRequest.builder()
                .url(mockUrl)
                .expiration(Instant.now().plusSeconds(900))
                .build();

        when(s3Presigner.presignGetObject(any())).thenReturn(mockPresignedRequest);

        // When
        PresignedUrlResponse response = presignedUrlService.generateDownloadUrl(fileKey);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUploadUrl()).isNotNull();
        assertThat(response.getFileKey()).isEqualTo(fileKey);
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("잘못된 파일 키로 다운로드 URL 생성 실패")
    void generateDownloadUrl_InvalidFileKey_Failure() {
        // Given: 경로 조작 시도
        String invalidKey = "../etc/passwd";

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateDownloadUrl(invalidKey))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file key");
    }

    @Test
    @DisplayName("uploads/로 시작하지 않는 파일 키로 실패")
    void generateDownloadUrl_NotStartsWithUploads_Failure() {
        // Given
        String invalidKey = "invalid/path/file.mp3";

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateDownloadUrl(invalidKey))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid file key");
    }

    @Test
    @DisplayName("빈 파일 키로 실패")
    void generateDownloadUrl_EmptyFileKey_Failure() {
        // Given
        String emptyKey = "";

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateDownloadUrl(emptyKey))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("File key cannot be empty");
    }

    @Test
    @DisplayName("경로 조작 시도 차단")
    void generateDownloadUrl_PathTraversalAttempt_Failure() {
        // Given
        String maliciousKey = "uploads/../../../etc/passwd";

        // When & Then
        assertThatThrownBy(() -> presignedUrlService.generateDownloadUrl(maliciousKey))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("path traversal attempt detected");
    }
}
