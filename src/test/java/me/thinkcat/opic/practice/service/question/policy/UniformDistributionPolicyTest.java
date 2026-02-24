package me.thinkcat.opic.practice.service.question.policy;

import me.thinkcat.opic.practice.entity.Question;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class UniformDistributionPolicyTest {

    private final UniformDistributionPolicy policy = new UniformDistributionPolicy();

    @Test
    void 카테고리3개_질문4개씩_5개_요청시_총5개_반환() {
        Map<Long, List<Question>> byCategory = new LinkedHashMap<>();
        byCategory.put(1L, makeQuestions(4));
        byCategory.put(2L, makeQuestions(4));
        byCategory.put(3L, makeQuestions(4));

        List<Question> result = policy.select(byCategory, 5);

        assertThat(result).hasSize(5);
    }

    @Test
    void 카테고리2개_나머지없을때_카테고리당_균등_분배() {
        Map<Long, List<Question>> byCategory = new LinkedHashMap<>();
        byCategory.put(1L, makeQuestions(6));
        byCategory.put(2L, makeQuestions(6));

        List<Question> result = policy.select(byCategory, 4);

        assertThat(result).hasSize(4);
    }

    @Test
    void 질문수보다_많이_요청하면_있는만큼만_반환() {
        Map<Long, List<Question>> byCategory = new LinkedHashMap<>();
        byCategory.put(1L, makeQuestions(2));

        List<Question> result = policy.select(byCategory, 5);

        assertThat(result).hasSize(2);
    }

    /**
     * 자연어: 빈 카테고리 맵으로 요청하면 빈 리스트가 반환된다.
     */
    @Test
    void 빈_카테고리맵은_빈_리스트_반환() {
        List<Question> result = policy.select(Map.of(), 5);

        assertThat(result).isEmpty();
    }

    private List<Question> makeQuestions(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> Question.builder()
                        .id((long) i)
                        .categoryId(1L)
                        .questionTypeId(1L)
                        .question("Q" + i)
                        .build())
                .toList();
    }
}
