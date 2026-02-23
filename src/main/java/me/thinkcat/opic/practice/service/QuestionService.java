package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.QuestionMapper;
import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionPracticeCountProjection;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final DrillAnswerRepository drillAnswerRepository;

    @Transactional(readOnly = true)
    public List<QuestionResponse> getAllQuestions(Long userId) {
        List<Question> questions = questionRepository.findAll();
        return toResponsesWithPracticeCount(questions, userId);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        return QuestionMapper.toResponse(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategoryId(Long categoryId, Long userId) {
        List<Question> questions = questionRepository.findByCategoryId(categoryId);
        return toResponsesWithPracticeCount(questions, userId);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByTypeId(Long typeId, Long userId) {
        List<Question> questions = questionRepository.findByQuestionTypeId(typeId);
        return toResponsesWithPracticeCount(questions, userId);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategoryIdAndTypeId(Long categoryId, Long typeId, Long userId) {
        List<Question> questions = questionRepository.findByCategoryIdAndQuestionTypeId(categoryId, typeId);
        return toResponsesWithPracticeCount(questions, userId);
    }

    private List<QuestionResponse> toResponsesWithPracticeCount(List<Question> questions, Long userId) {
        if (questions.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        String successCode = UploadStatus.SUCCESS.getCode();

        Map<Long, Long> sessionCounts = answerRepository
                .countByQuestionIdsAndUserId(questionIds, userId, successCode).stream()
                .collect(Collectors.toMap(
                        QuestionPracticeCountProjection::getQuestionId,
                        QuestionPracticeCountProjection::getCount));

        Map<Long, Long> drillCounts = drillAnswerRepository
                .countByQuestionIdsAndUserId(questionIds, userId, successCode).stream()
                .collect(Collectors.toMap(
                        QuestionPracticeCountProjection::getQuestionId,
                        QuestionPracticeCountProjection::getCount));

        return questions.stream()
                .map(q -> {
                    int count = (int) (sessionCounts.getOrDefault(q.getId(), 0L)
                            + drillCounts.getOrDefault(q.getId(), 0L));
                    return QuestionMapper.toResponse(q, count);
                })
                .collect(Collectors.toList());
    }
}
