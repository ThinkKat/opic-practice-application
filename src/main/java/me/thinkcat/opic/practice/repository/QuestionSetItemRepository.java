package me.thinkcat.opic.practice.repository;

import me.thinkcat.opic.practice.entity.QuestionSetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionSetItemRepository extends JpaRepository<QuestionSetItem, Long> {
    List<QuestionSetItem> findByQuestionSetIdOrderByOrderIndex(Long questionSetId);

    void deleteByQuestionSetId(Long questionSetId);
}
