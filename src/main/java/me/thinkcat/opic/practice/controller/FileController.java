package me.thinkcat.opic.practice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.request.GenerateUploadUrlRequest;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.CommonResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.security.annotation.AuthUser;
import me.thinkcat.opic.practice.security.AuthUserInfo;
import me.thinkcat.opic.practice.service.AudioStreamingService;
import me.thinkcat.opic.practice.service.PresignedUrlService;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

//@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File", description = "파일 업로드/다운로드 API")
public class FileController {

    private final PresignedUrlService presignedUrlService;
    private final AudioStreamingService audioStreamingService;

    /**
     * 업로드용 Presigned URL 발급
     *
     * @param request 파일 메타데이터 (fileName, contentType, contentLength)
     * @param user 인증 정보
     * @return Presigned URL 및 필수 헤더 정보
     */
    @PostMapping("/upload-url")
    @Operation(summary = "업로드용 Presigned URL 발급", description = "S3에 파일을 업로드하기 위한 임시 URL을 발급합니다.")
    public ResponseEntity<CommonResponse<PresignedUrlResponse>> generateUploadUrl(
            @Valid @RequestBody GenerateUploadUrlRequest request,
            @AuthUser AuthUserInfo user) {

        log.info("User {} requested upload URL for file: {}", user.getUserId(), request.getFileName());

        // 파일 키 생성 (FileController가 책임)
        String fileKey = presignedUrlService.generateSimpleFileKey(request.getFileName());

        // Presigned URL 발급 요청
        PresignedUrlRequest presignedRequest = new PresignedUrlRequest(
                fileKey,
                request.getContentType(),
                request.getContentLength()
        );

        PresignedUrlResponse response = presignedUrlService.generateUploadUrl(presignedRequest);

        CommonResponse<PresignedUrlResponse> commonResponse =
                CommonResponse.<PresignedUrlResponse>builder()
                        .success(true)
                        .result(response)
                        .message("Upload URL generated successfully")
                        .build();

        return ResponseEntity.ok(commonResponse);
    }

    /**
     * 다운로드용 Presigned URL 발급
     *
     * @param fileKey S3 파일 키 (예: uploads/abc-123.mp3)
     * @param authentication 인증 정보
     * @return Presigned URL
     */
    @GetMapping("/download-url")
    @Operation(summary = "다운로드용 Presigned URL 발급", description = "S3에서 파일을 다운로드하기 위한 임시 URL을 발급합니다.")
    public ResponseEntity<CommonResponse<PresignedUrlResponse>> generateDownloadUrl(
            @RequestParam("fileKey") String fileKey,
            @AuthUser AuthUserInfo user) {

        log.info("User {} requested download URL for file: {}", user.getUserId(), fileKey);

        PresignedUrlResponse response = presignedUrlService.generateDownloadUrl(fileKey);

        CommonResponse<PresignedUrlResponse> commonResponse =
                CommonResponse.<PresignedUrlResponse>builder()
                        .success(true)
                        .result(response)
                        .message("Download URL generated successfully")
                        .build();

        return ResponseEntity.ok(commonResponse);
    }

    /**
     * 로컬 파일 스트리밍 (HTTP Range Request 지원)
     *
     * @param request HTTP 요청 (경로 추출용)
     * @param headers HTTP 헤더 (Range 헤더 포함)
     * @param authentication 인증 정보
     * @return 파일의 일부 또는 전체 (206 Partial Content)
     */
    @GetMapping("/stream/**")
    @Operation(summary = "로컬 파일 스트리밍", description = "로컬에 저장된 파일을 HTTP Range Request를 지원하여 스트리밍합니다.")
    public ResponseEntity<ResourceRegion> streamAudioFile(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers,
            @AuthUser AuthUserInfo user) throws IOException {

        // 경로 추출: /api/v1/files/stream/answers/1/abc.mp3 -> answers/1/abc.mp3
        String fullPath = request.getRequestURI();
        String[] parts = fullPath.split("/stream/", 2);
        if (parts.length < 2) {
            throw new ValidationException("Invalid file path");
        }
        String filePath = parts[1];

        // Service에서 권한 확인 및 파일 스트리밍 처리
        ResourceRegion region = audioStreamingService.streamFile(filePath, user.getUserId(), headers);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .header("Accept-Ranges", "bytes")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(region);
    }
}
