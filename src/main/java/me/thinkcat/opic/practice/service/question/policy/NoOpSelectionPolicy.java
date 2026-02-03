package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoOpSelectionPolicy implements QuestionSelectionPolicy {

    @Override
    public List<Question> select(List<Question> availableQuestions, int count) {
        int actualCount = Math.min(count, availableQuestions.size());
        return availableQuestions.subList(0, actualCount);
    }
}
