package me.thinkcat.opic.practice.controller;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.config.security.AuthUserInfo;
import me.thinkcat.opic.practice.config.security.annotation.AuthUser;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<QuestionResponse>>> getQuestions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long typeId,
            @AuthUser AuthUserInfo user) {

        List<QuestionResponse> questions;

        if (categoryId != null && typeId != null) {
            questions = questionService.getQuestionsByCategoryIdAndTypeId(categoryId, typeId, user.getUserId());
        } else if (categoryId != null) {
            questions = questionService.getQuestionsByCategoryId(categoryId, user.getUserId());
        } else if (typeId != null) {
            questions = questionService.getQuestionsByTypeId(typeId, user.getUserId());
        } else {
            questions = questionService.getAllQuestions(user.getUserId());
        }

        CommonResponse<List<QuestionResponse>> response = CommonResponse.<List<QuestionResponse>>builder()
                .success(true)
                .result(questions)
                .message("Questions retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<QuestionResponse>> getQuestionById(@PathVariable Long id) {
        QuestionResponse question = questionService.getQuestionById(id);

        CommonResponse<QuestionResponse> response = CommonResponse.<QuestionResponse>builder()
                .success(true)
                .result(question)
                .message("Question retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<CommonResponse<QuestionResponse>> getQuestionByIdInternal(@PathVariable Long id) {
        QuestionResponse question = questionService.getQuestionById(id);

        CommonResponse<QuestionResponse> response = CommonResponse.<QuestionResponse>builder()
                .success(true)
                .result(question)
                .message("Question retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
