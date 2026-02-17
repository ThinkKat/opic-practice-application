package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UniformDistributionPolicy implements QuestionSelectionPolicy {

    @Override
    public List<Question> select(Map<Long, List<Question>> questionsByCategory, int count) {
        if (questionsByCategory.isEmpty()) {
            return List.of();
        }

        List<Long> categoryIds = new ArrayList<>(questionsByCategory.keySet());
        int categoryCount = categoryIds.size();
        int basePerCategory = count / categoryCount;
        int remainder = count % categoryCount;

        List<Question> result = new ArrayList<>();

        for (int i = 0; i < categoryCount; i++) {
            Long categoryId = categoryIds.get(i);
            List<Question> questions = new ArrayList<>(questionsByCategory.get(categoryId));
            Collections.shuffle(questions);

            int pick = basePerCategory + (i < remainder ? 1 : 0);
            int actualPick = Math.min(pick, questions.size());
            result.addAll(questions.subList(0, actualPick));
        }

        return result;
    }
}
