package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NoOpSelectionPolicy implements QuestionSelectionPolicy {

    @Override
    public List<Question> select(Map<Long, List<Question>> questionsByCategory, int count) {
        List<Question> allQuestions = questionsByCategory.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        int actualCount = Math.min(count, allQuestions.size());
        return allQuestions.subList(0, actualCount);
    }
}
