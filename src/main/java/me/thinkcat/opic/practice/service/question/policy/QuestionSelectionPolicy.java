package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;

import java.util.List;
import java.util.Map;

public interface QuestionSelectionPolicy {
    List<Question> select(Map<Long, List<Question>> questionsByCategory, int count);
}
