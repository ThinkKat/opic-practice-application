package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.request.SessionCreateRequest;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.dto.response.SessionResponse;
import me.thinkcat.opic.practice.security.annotation.AuthUser;
import me.thinkcat.opic.practice.security.AuthUserInfo;
import me.thinkcat.opic.practice.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<CommonResponse<SessionResponse>> createSession(
            @Valid @RequestBody SessionCreateRequest request,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.createSession(userId, request);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session created successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<CommonResponse<SessionResponse>> startSession(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.startSession(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session started successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<CommonResponse<SessionResponse>> pauseSession(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.pauseSession(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session paused successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<CommonResponse<SessionResponse>> resumeSession(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.resumeSession(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session resumed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CommonResponse<SessionResponse>> completeSession(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.completeSession(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session completed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommonResponse<SessionResponse>> cancelSession(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.cancelSession(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session cancelled successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<SessionResponse>>> getUserSessions(
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        List<SessionResponse> sessions = sessionService.getUserSessions(userId);

        CommonResponse<List<SessionResponse>> response = CommonResponse.<List<SessionResponse>>builder()
                .success(true)
                .result(sessions)
                .message("Sessions retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<SessionResponse>> getSessionById(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        SessionResponse sessionResponse = sessionService.getSessionById(id, userId);

        CommonResponse<SessionResponse> response = CommonResponse.<SessionResponse>builder()
                .success(true)
                .result(sessionResponse)
                .message("Session retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<CommonResponse<List<QuestionResponse>>> getSessionQuestions(
            @PathVariable Long id,
            @AuthUser AuthUserInfo user) {

        Long userId = user.getUserId();
        List<QuestionResponse> questions = sessionService.getSessionQuestions(id, userId);

        CommonResponse<List<QuestionResponse>> response = CommonResponse.<List<QuestionResponse>>builder()
                .success(true)
                .result(questions)
                .message("Session questions retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

}
