package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByCategoryId(Long categoryId);

    List<Question> findByQuestionTypeId(Long questionTypeId);

    List<Question> findByCategoryIdAndQuestionTypeId(Long categoryId, Long questionTypeId);
}
