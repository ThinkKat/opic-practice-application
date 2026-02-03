package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;

import java.util.List;

public interface QuestionSelectionPolicy {
    List<Question> select(List<Question> availableQuestions, int count);
}
