package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.request.CompleteAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.response.CommonResponse;
import me.thinkcat.opic.practice.service.AnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @PostMapping
    public ResponseEntity<CommonResponse<AnswerResponse>> createAnswer(
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "durationMs", required = false) Integer durationMs,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        AnswerResponse answerResponse = answerService.createAnswer(
                userId, sessionId, questionId, audioFile, durationMs);

        CommonResponse<AnswerResponse> response = CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer created successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-upload")
    public ResponseEntity<CommonResponse<AnswerResponse>> completeAnswerUpload(
            @Valid @RequestBody CompleteAnswerUploadRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        AnswerResponse answerResponse = answerService.completeAnswerUpload(
                userId,
                request.getSessionId(),
                request.getQuestionId(),
                request.getFileKey(),
                request.getMimeType(),
                request.getDurationMs());

        CommonResponse<AnswerResponse> response = CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer upload completed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<CommonResponse<List<AnswerResponse>>> getSessionAnswers(
            @PathVariable Long sessionId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        List<AnswerResponse> answers = answerService.getSessionAnswers(sessionId, userId);

        CommonResponse<List<AnswerResponse>> response = CommonResponse.<List<AnswerResponse>>builder()
                .success(true)
                .result(answers)
                .message("Answers retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<AnswerResponse>> getAnswerById(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        AnswerResponse answerResponse = answerService.getAnswerById(id, userId);

        CommonResponse<AnswerResponse> response = CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> deleteAnswer(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        answerService.deleteAnswer(id, userId);

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Answer deleted successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        // TODO: JWT에서 userId 추출하는 로직 구현
        return 1L; // Placeholder
    }
}
