package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.QuestionTypeMapper;
import me.thinkcat.opic.practice.dto.response.QuestionTypeResponse;
import me.thinkcat.opic.practice.entity.QuestionType;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.repository.QuestionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionTypeService {

    private final QuestionTypeRepository questionTypeRepository;

    @Transactional(readOnly = true)
    public List<QuestionTypeResponse> getAllQuestionTypes() {
        return questionTypeRepository.findAll().stream()
                .map(QuestionTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionTypeResponse getQuestionTypeById(Long id) {
        QuestionType questionType = questionTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionType not found with id: " + id));
        return QuestionTypeMapper.toResponse(questionType);
    }
}
