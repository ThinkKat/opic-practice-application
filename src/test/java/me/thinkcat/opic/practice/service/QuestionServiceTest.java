package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionPracticeCountProjection;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private DrillAnswerRepository drillAnswerRepository;
    @InjectMocks private QuestionService questionService;

    private final Long userId = 10L;
    private final String successCode = UploadStatus.SUCCESS.getCode();

    @Test
    void 세션2회_드릴3회인_질문의_practiceCount는_5() {
        Long questionId = 1L;
        Question question = makeQuestion(questionId);

        given(questionRepository.findAll()).willReturn(List.of(question));
        given(answerRepository.countByQuestionIdsAndUserId(List.of(questionId), userId, successCode))
                .willReturn(List.of(projection(questionId, 2L)));
        given(drillAnswerRepository.countByQuestionIdsAndUserId(List.of(questionId), userId, successCode))
                .willReturn(List.of(projection(questionId, 3L)));

        List<QuestionResponse> result = questionService.getAllQuestions(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPracticeCount()).isEqualTo(5);
    }

    @Test
    void 연습이력이_없는_질문의_practiceCount는_0() {
        Long questionId = 2L;
        Question question = makeQuestion(questionId);

        given(questionRepository.findAll()).willReturn(List.of(question));
        given(answerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of());
        given(drillAnswerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of());

        List<QuestionResponse> result = questionService.getAllQuestions(userId);

        assertThat(result.get(0).getPracticeCount()).isEqualTo(0);
    }

    @Test
    void 드릴이력만_있는_질문은_드릴횟수만_집계() {
        Long questionId = 4L;
        Question question = makeQuestion(questionId);

        given(questionRepository.findAll()).willReturn(List.of(question));
        given(answerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of());
        given(drillAnswerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of(projection(questionId, 5L)));

        List<QuestionResponse> result = questionService.getAllQuestions(userId);

        assertThat(result.get(0).getPracticeCount()).isEqualTo(5);
    }

    @Test
    void 세션이력만_있는_질문은_세션횟수만_집계() {
        Long questionId = 3L;
        Question question = makeQuestion(questionId);

        given(questionRepository.findAll()).willReturn(List.of(question));
        given(answerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of(projection(questionId, 4L)));
        given(drillAnswerRepository.countByQuestionIdsAndUserId(any(), any(), any()))
                .willReturn(List.of());

        List<QuestionResponse> result = questionService.getAllQuestions(userId);

        assertThat(result.get(0).getPracticeCount()).isEqualTo(4);
    }

    private Question makeQuestion(Long id) {
        return Question.builder()
                .id(id)
                .categoryId(1L)
                .questionTypeId(1L)
                .question("Q" + id)
                .build();
    }

    private QuestionPracticeCountProjection projection(Long questionId, Long count) {
        return new QuestionPracticeCountProjection() {
            @Override public Long getQuestionId() { return questionId; }
            @Override public Long getCount() { return count; }
        };
    }
}
