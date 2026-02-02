package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.QuestionMapper;
import me.thinkcat.opic.practice.dto.response.QuestionResponse;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<QuestionResponse> getAllQuestions() {
        return questionRepository.findAll().stream()
                .map(QuestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        return QuestionMapper.toResponse(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategoryId(Long categoryId) {
        return questionRepository.findByCategoryId(categoryId).stream()
                .map(QuestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByTypeId(Long typeId) {
        return questionRepository.findByQuestionTypeId(typeId).stream()
                .map(QuestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategoryIdAndTypeId(Long categoryId, Long typeId) {
        return questionRepository.findByCategoryIdAndQuestionTypeId(categoryId, typeId).stream()
                .map(QuestionMapper::toResponse)
                .collect(Collectors.toList());
    }
}
