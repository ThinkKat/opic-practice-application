package me.thinkcat.opic.practice.controller;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.response.CommonResponse;
import me.thinkcat.opic.practice.dto.response.QuestionTypeResponse;
import me.thinkcat.opic.practice.service.QuestionTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/types")
@RequiredArgsConstructor
public class QuestionTypeController {

    private final QuestionTypeService questionTypeService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<QuestionTypeResponse>>> getAllQuestionTypes() {
        List<QuestionTypeResponse> questionTypes = questionTypeService.getAllQuestionTypes();

        CommonResponse<List<QuestionTypeResponse>> response = CommonResponse.<List<QuestionTypeResponse>>builder()
                .success(true)
                .result(questionTypes)
                .message("Question types retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<QuestionTypeResponse>> getQuestionTypeById(@PathVariable Long id) {
        QuestionTypeResponse questionType = questionTypeService.getQuestionTypeById(id);

        CommonResponse<QuestionTypeResponse> response = CommonResponse.<QuestionTypeResponse>builder()
                .success(true)
                .result(questionType)
                .message("Question type retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
