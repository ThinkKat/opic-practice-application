package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.request.CompleteAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.request.PrepareAnswerUploadRequest;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.PrepareAnswerUploadResponse;
import me.thinkcat.opic.practice.security.annotation.AuthUser;
import me.thinkcat.opic.practice.security.AuthUserInfo;
import me.thinkcat.opic.practice.service.AnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
                request.getAnswerId(),
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

    @GetMapping("/{id}/playback")
    public ResponseEntity<CommonResponse<AnswerResponse>> getAnswerForPlayback(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        AnswerResponse answerResponse = answerService.getAnswerForPlayback(id, userId);

        CommonResponse<AnswerResponse> response = CommonResponse.<AnswerResponse>builder()
                .success(true)
                .result(answerResponse)
                .message("Answer ready for playback")
                .build();

        return ResponseEntity.ok(response);
    }
}
