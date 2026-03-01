package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.config.security.AuthUserInfo;
import me.thinkcat.opic.practice.config.security.annotation.AuthUser;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.request.PrepareDrillAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.request.SubmitDrillAnswerRequest;
import me.thinkcat.opic.practice.dto.request.UpdateDrillFeedbackRequest;
import me.thinkcat.opic.practice.dto.request.UpdateDrillTranscriptionRequest;
import me.thinkcat.opic.practice.dto.request.UpdateFeedbackStatusRequest;
import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareDrillAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.QuestionPracticeHistoryResponse;
import me.thinkcat.opic.practice.dto.response.RecentDrillQuestionResponse;
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

@RestController
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

        CommonResponse<PrepareDrillAnswerUploadResponse> commonResponse =
                CommonResponse.<PrepareDrillAnswerUploadResponse>builder()
                        .success(true)
                        .result(response)
                        .message("Drill answer upload prepared successfully")
                        .build();

        return ResponseEntity.ok(commonResponse);
    }

    @PostMapping("/submit")
    public ResponseEntity<CommonResponse<DrillAnswerResponse>> submitDrillAnswer(
            @Valid @RequestBody SubmitDrillAnswerRequest request,
            @AuthUser AuthUserInfo user) {

        DrillAnswerResponse answerResponse = drillAnswerService.submitDrillAnswer(
                user.getUserId(),
                user.getRole(),
                request.getDrillAnswerId(),
                request.getDurationMs());

        CommonResponse<DrillAnswerResponse> response = CommonResponse.<DrillAnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Drill answer submitted successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recently-practiced-questions")
    public ResponseEntity<CommonResponse<List<RecentDrillQuestionResponse>>> getRecentlyPracticedQuestions(
            @AuthUser AuthUserInfo user) {

        List<RecentDrillQuestionResponse> result = drillAnswerService
                .getRecentlyPracticedQuestions(user.getUserId());

        CommonResponse<List<RecentDrillQuestionResponse>> response =
                CommonResponse.<List<RecentDrillQuestionResponse>>builder()
                        .success(true)
                        .result(result)
                        .message("Recently practiced questions retrieved successfully")
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{questionId}")
    public ResponseEntity<CommonResponse<List<DrillAnswerResponse>>> getDrillAnswersByQuestion(
            @PathVariable Long questionId,
            @AuthUser AuthUserInfo user) {

        List<DrillAnswerResponse> answers = drillAnswerService.getDrillAnswersByQuestion(
                user.getUserId(), questionId);

        CommonResponse<List<DrillAnswerResponse>> response = CommonResponse.<List<DrillAnswerResponse>>builder()
                .success(true)
                .result(answers)
                .message("Drill answers retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/questions/{questionId}/practice-history")
    public ResponseEntity<CommonResponse<QuestionPracticeHistoryResponse>> getQuestionPracticeHistory(
            @PathVariable Long questionId,
            @AuthUser AuthUserInfo user) {

        QuestionPracticeHistoryResponse historyResponse = questionPracticeHistoryService
                .getQuestionPracticeHistory(questionId, user.getUserId());

        CommonResponse<QuestionPracticeHistoryResponse> response =
                CommonResponse.<QuestionPracticeHistoryResponse>builder()
                        .success(true)
                        .result(historyResponse)
                        .message("Question practice history retrieved successfully")
                        .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/internal/feedback-status")
    public ResponseEntity<CommonResponse<Void>> updateDrillFeedbackStatus(
            @Valid @RequestBody UpdateFeedbackStatusRequest request) {

        drillAnswerService.updateFeedbackFailed(request.getAudioUrl(), request.getReason());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Drill feedback status updated successfully")
                .build();

        return ResponseEntity.ok(response);
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

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Drill transcription updated successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/internal/feedback")
    public ResponseEntity<CommonResponse<Void>> updateDrillFeedback(
            @Valid @RequestBody UpdateDrillFeedbackRequest request) {

        drillAnswerService.updateFeedback(request.getAudioUrl(), request.getFeedback());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Drill feedback updated successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
