package me.thinkcat.opic.practice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.CommonResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.service.PresignedUrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File", description = "파일 업로드/다운로드 API")
public class FileController {

    private final PresignedUrlService presignedUrlService;

    /**
     * 업로드용 Presigned URL 발급
     *
     * @param request 파일 메타데이터 (fileName, contentType, contentLength)
     * @param authentication 인증 정보
     * @return Presigned URL 및 필수 헤더 정보
     */
    @PostMapping("/upload-url")
    @Operation(summary = "업로드용 Presigned URL 발급", description = "S3에 파일을 업로드하기 위한 임시 URL을 발급합니다.")
    public ResponseEntity<CommonResponse<PresignedUrlResponse>> generateUploadUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("User {} requested upload URL for file: {}", userId, request.getFileName());

        PresignedUrlResponse response = presignedUrlService.generateUploadUrl(request);

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
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("User {} requested download URL for file: {}", userId, fileKey);

        PresignedUrlResponse response = presignedUrlService.generateDownloadUrl(fileKey);

        CommonResponse<PresignedUrlResponse> commonResponse =
                CommonResponse.<PresignedUrlResponse>builder()
                        .success(true)
                        .result(response)
                        .message("Download URL generated successfully")
                        .build();

        return ResponseEntity.ok(commonResponse);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        // TODO: JwtTokenProvider와 연동하여 실제 userId 추출
        // 현재는 임시로 1L 반환
        return 1L;
    }
}
