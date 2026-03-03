package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.request.CompleteAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.request.PrepareAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.request.S3UploadDetectedRequest;
import me.thinkcat.opic.practice.dto.request.UpdateFeedbackRequest;
import me.thinkcat.opic.practice.dto.request.UpdateFeedbackStatusRequest;
import me.thinkcat.opic.practice.dto.request.UpdateTranscriptionRequest;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.PrepareAnswerUploadResponse;
import me.thinkcat.opic.practice.config.security.annotation.AuthUser;
import me.thinkcat.opic.practice.config.security.AuthUserInfo;
import me.thinkcat.opic.practice.service.AnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping("/prepare")
    public ResponseEntity<CommonResponse<PrepareAnswerUploadResponse>> prepareAnswerUpload(
            @Valid @RequestBody PrepareAnswerUploadRequest request,
            @AuthUser AuthUserInfo user) {

        PrepareAnswerUploadResponse response = answerService.prepareAnswerUpload(
                user.getUserId(),
                request.getSessionId(),
                request.getQuestionId(),
                request.getFileName(),
                request.getContentType(),
                request.getContentLength());

        CommonResponse<PrepareAnswerUploadResponse> commonResponse =
                CommonResponse.<PrepareAnswerUploadResponse>builder()
                        .success(true)
                        .result(response)
                        .message("Upload prepared successfully")
                        .build();

        return ResponseEntity.ok(commonResponse);
    }

    @PostMapping("/complete-upload")
    public ResponseEntity<CommonResponse<AnswerResponse>> completeAnswerUpload(
            @Valid @RequestBody CompleteAnswerUploadRequest request,
            @AuthUser AuthUserInfo user) {

        AnswerResponse answerResponse = answerService.completeAnswerUpload(
                user.getUserId(),
                user.getRole(),
                request.getAnswerId(),
                request.getDurationMs());

        CommonResponse<AnswerResponse> response = CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer upload completed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/internal/s3-upload-detected")
    public ResponseEntity<CommonResponse<Void>> handleS3UploadDetected(
            @Valid @RequestBody S3UploadDetectedRequest request) {

        answerService.handleS3UploadDetected(request.getFileKey());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("S3 upload detected processed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/internal/transcription")
    public ResponseEntity<CommonResponse<Void>> updateTranscription(
            @Valid @RequestBody UpdateTranscriptionRequest request) {

        answerService.updateTranscription(
                request.getAudioUrl(),
                request.getTranscription(),
                request.getWordSegments(),
                request.getPauseAnalysis(),
                request.getDuration());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Transcription updated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/internal/feedback")
    public ResponseEntity<CommonResponse<Void>> updateFeedback(
            @Valid @RequestBody UpdateFeedbackRequest request) {

        answerService.updateFeedback(request.getAudioUrl(), request.getFeedback());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Feedback updated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/internal/feedback-status")
    public ResponseEntity<CommonResponse<Void>> updateFeedbackStatus(
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {

        answerService.updateFeedbackFailed(request.getAudioUrl(), request.getReason());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Feedback status updated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CommonResponse<List<AnswerResponse>>> getSessionAnswers(
            @PathVariable Long sessionId,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        List<AnswerResponse> answers = answerService.getSessionAnswers(sessionId, userId);

        CommonResponse<List<AnswerResponse>> response = CommonResponse.<List<AnswerResponse>>builder()
                .success(true)
                .result(answers)
                .message("Answers retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

}
