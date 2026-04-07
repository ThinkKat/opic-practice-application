package me.thinkcat.opic.practice.controller.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.config.security.AuthUserInfo;
import me.thinkcat.opic.practice.config.security.annotation.AuthUser;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.mapper.v1.DrillAnswerMapper;
import me.thinkcat.opic.practice.dto.request.PrepareDrillAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.request.S3UploadDetectedRequest;
import me.thinkcat.opic.practice.dto.request.SubmitDrillAnswerRequest;
import me.thinkcat.opic.practice.dto.request.UpdateDrillFeedbackRequest;
import me.thinkcat.opic.practice.dto.request.UpdateDrillTranscriptionRequest;
import me.thinkcat.opic.practice.dto.request.UpdateFeedbackStatusRequest;
import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareDrillAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.QuestionPracticeHistoryResponse;
import me.thinkcat.opic.practice.dto.response.RecentDrillQuestionResponse;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.service.DrillAnswerService;
import me.thinkcat.opic.practice.service.QuestionPracticeHistoryService;
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

@RestController("v1DrillAnswerController")
@RequestMapping("/api/v1/drill-answers")
@RequiredArgsConstructor
public class DrillAnswerController {

    private final DrillAnswerService drillAnswerService;
    private final QuestionPracticeHistoryService questionPracticeHistoryService;

    @PostMapping("/prepare")
    public ResponseEntity<CommonResponse<PrepareDrillAnswerUploadResponse>> prepareDrillAnswerUpload(
            @Valid @RequestBody PrepareDrillAnswerUploadRequest request,
            @AuthUser AuthUserInfo user) {

        PrepareDrillAnswerUploadResponse response = drillAnswerService.prepareDrillAnswerUpload(
                user.getUserId(),
                request.getQuestionId(),
                request.getFileName(),
                request.getContentType(),
                request.getContentLength());

        return ResponseEntity.ok(CommonResponse.<PrepareDrillAnswerUploadResponse>builder()
                .success(true)
                .result(response)
                .message("Drill answer upload prepared successfully")
                .build());
    }

    @PostMapping("/submit")
    public ResponseEntity<CommonResponse<DrillAnswerResponse>> submitDrillAnswer(
            @Valid @RequestBody SubmitDrillAnswerRequest request,
            @AuthUser AuthUserInfo user) {

        DrillAnswer answer = drillAnswerService.submitDrillAnswer(
                user.getUserId(),
                user.getRole(),
                request.getDrillAnswerId(),
                request.getDurationMs());

        DrillAnswerResponse answerResponse = DrillAnswerMapper.toResponse(
                answer, drillAnswerService.resolveAudioUrl(answer));

        return ResponseEntity.ok(CommonResponse.<DrillAnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Drill answer submitted successfully")
                .build());
    }

    @GetMapping("/recently-practiced-questions")
    public ResponseEntity<CommonResponse<List<RecentDrillQuestionResponse>>> getRecentlyPracticedQuestions(
            @AuthUser AuthUserInfo user) {

        List<RecentDrillQuestionResponse> result = drillAnswerService
                .getRecentlyPracticedQuestions(user.getUserId());

        return ResponseEntity.ok(CommonResponse.<List<RecentDrillQuestionResponse>>builder()
                .success(true)
                .result(result)
                .message("Recently practiced questions retrieved successfully")
                .build());
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<CommonResponse<List<DrillAnswerResponse>>> getDrillAnswersByQuestion(
            @PathVariable Long questionId,
            @AuthUser AuthUserInfo user) {

        List<DrillAnswerResponse> answers = drillAnswerService
                .getDrillAnswersByQuestion(user.getUserId(), questionId)
                .stream()
                .map(a -> DrillAnswerMapper.toResponse(a, drillAnswerService.resolveAudioUrl(a)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(CommonResponse.<List<DrillAnswerResponse>>builder()
                .success(true)
                .result(answers)
                .message("Drill answers retrieved successfully")
                .build());
    }

    @GetMapping("/questions/{questionId}/practice-history")
    public ResponseEntity<CommonResponse<QuestionPracticeHistoryResponse>> getQuestionPracticeHistory(
            @PathVariable Long questionId,
            @AuthUser AuthUserInfo user) {

        QuestionPracticeHistoryResponse historyResponse = questionPracticeHistoryService
                .getQuestionPracticeHistory(questionId, user.getUserId());

        return ResponseEntity.ok(CommonResponse.<QuestionPracticeHistoryResponse>builder()
                .success(true)
                .result(historyResponse)
                .message("Question practice history retrieved successfully")
                .build());
    }

    @PostMapping("/internal/s3-upload-detected")
    public ResponseEntity<CommonResponse<Void>> handleS3UploadDetected(
            @Valid @RequestBody S3UploadDetectedRequest request) {

        drillAnswerService.handleS3UploadDetected(request.getFileKey());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Drill S3 upload detected processed successfully")
                .build());
    }

    @PatchMapping("/internal/feedback-status")
    public ResponseEntity<CommonResponse<Void>> updateDrillFeedbackStatus(
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {

        drillAnswerService.updateFeedbackFailed(request.getAudioUrl(), request.getReason());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Drill feedback status updated successfully")
                .build());
    }

    @PatchMapping("/internal/transcription")
    public ResponseEntity<CommonResponse<Void>> updateDrillTranscription(
            @Valid @RequestBody UpdateDrillTranscriptionRequest request) {

        drillAnswerService.updateTranscription(
                request.getAudioUrl(),
                request.getTranscription(),
                request.getWordSegments(),
                request.getPauseAnalysis(),
                request.getDuration());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Drill transcription updated successfully")
                .build());
    }

    @PatchMapping("/internal/feedback")
    public ResponseEntity<CommonResponse<Void>> updateDrillFeedback(
            @Valid @RequestBody UpdateDrillFeedbackRequest request) {

        drillAnswerService.updateFeedback(request.getAudioUrl(), request.getFeedback());

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .success(true)
                .message("Drill feedback updated successfully")
                .build());
    }

    @PostMapping("/{answerId}/retry-feedback")
    public ResponseEntity<CommonResponse<DrillAnswerResponse>> retryFeedback(
            @PathVariable Long answerId,
            @AuthUser AuthUserInfo user) {

        DrillAnswer answer = drillAnswerService.retryFeedback(answerId, user.getUserId());
        DrillAnswerResponse answerResponse = DrillAnswerMapper.toResponse(
                answer, drillAnswerService.resolveAudioUrl(answer));

        return ResponseEntity.ok(CommonResponse.<DrillAnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Re-invoke Feedback successfully")
                .build());
    }
}
