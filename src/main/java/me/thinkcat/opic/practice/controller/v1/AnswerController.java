package me.thinkcat.opic.practice.controller.v1;

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
import me.thinkcat.opic.practice.dto.mapper.v1.AnswerMapper;
import me.thinkcat.opic.practice.entity.Answer;
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
import java.util.stream.Collectors;

@RestController("v1AnswerController")
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

        return ResponseEntity.ok(CommonResponse.<PrepareAnswerUploadResponse>builder()
                .success(true)
                .result(response)
                .message("Upload prepared successfully")
                .build());
    }

    @PostMapping("/complete-upload")
    public ResponseEntity<CommonResponse<AnswerResponse>> completeAnswerUpload(
            @Valid @RequestBody CompleteAnswerUploadRequest request,
            @AuthUser AuthUserInfo user) {

        Answer answer = answerService.completeAnswerUpload(
                user.getUserId(),
                user.getRole(),
                request.getAnswerId(),
                request.getDurationMs());

        AnswerResponse answerResponse = AnswerMapper.toResponse(answer, answerService.resolveAudioUrl(answer));

        return ResponseEntity.ok(CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer upload completed successfully")
                .build());
    }

    @PostMapping("/internal/s3-upload-detected")
    public ResponseEntity<CommonResponse<Void>> handleS3UploadDetected(
            @Valid @RequestBody S3UploadDetectedRequest request) {

        answerService.handleS3UploadDetected(request.getFileKey());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("S3 upload detected processed successfully")
                .build());
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

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Transcription updated successfully")
                .build());
    }

    @PatchMapping("/internal/feedback")
    public ResponseEntity<CommonResponse<Void>> updateFeedback(
            @Valid @RequestBody UpdateFeedbackRequest request) {

        answerService.updateFeedback(request.getAudioUrl(), request.getFeedback());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Feedback updated successfully")
                .build());
    }

    @PatchMapping("/internal/feedback-status")
    public ResponseEntity<CommonResponse<Void>> updateFeedbackStatus(
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {

        answerService.updateFeedbackFailed(request.getAudioUrl(), request.getReason());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Feedback status updated successfully")
                .build());
    }

    @PostMapping("/{answerId}/retry-feedback")
    public ResponseEntity<CommonResponse<AnswerResponse>> retryFeedback(
            @PathVariable Long answerId,
            @AuthUser AuthUserInfo user) {

        Answer answer = answerService.retryFeedback(answerId, user.getUserId());
        AnswerResponse answerResponse = AnswerMapper.toResponse(answer, answerService.resolveAudioUrl(answer));

        return ResponseEntity.ok(CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Re-invoke Feedback successfully")
                .build());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CommonResponse<List<AnswerResponse>>> getSessionAnswers(
            @PathVariable Long sessionId,
            @AuthUser AuthUserInfo user) {

        List<AnswerResponse> answers = answerService.getSessionAnswers(sessionId, user.getUserId())
                .stream()
                .map(a -> AnswerMapper.toResponse(a, answerService.resolveAudioUrl(a)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(CommonResponse.<List<AnswerResponse>>builder()
                .success(true)
                .result(answers)
                .message("Answers retrieved successfully")
                .build());
    }
}
