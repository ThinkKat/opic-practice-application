package me.thinkcat.opic.practice.service.question;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.service.question.policy.QuestionSelectionPolicy;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionSelector {

    private final QuestionRepository questionRepository;

    public List<Question> selectQuestions(
            List<Long> categoryIds,
            int count,
            QuestionSelectionPolicy policy) {

        // 카테고리별 질문 조회 (순서 유지)
        Map<Long, List<Question>> questionsByCategory = new LinkedHashMap<>();
        for (Long categoryId : categoryIds) {
            List<Question> questions = questionRepository.findByCategoryId(categoryId);
            if (!questions.isEmpty()) {
                questionsByCategory.put(categoryId, questions);
            }
        }

        if (questionsByCategory.isEmpty()) {
            throw new ValidationException("No questions available for selected categories");
        }

        // 정책에 따라 선택
        return policy.select(questionsByCategory, count);
    }
}
