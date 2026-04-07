package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.v2.AnswerMapper;
import me.thinkcat.opic.practice.dto.mapper.CategoryMapper;
import me.thinkcat.opic.practice.dto.mapper.v2.DrillAnswerMapper;
import me.thinkcat.opic.practice.dto.mapper.QuestionMapper;
import me.thinkcat.opic.practice.dto.mapper.QuestionTypeMapper;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.dto.response.QuestionPracticeHistoryResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.Category;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.entity.QuestionType;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.CategoryRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.QuestionTypeRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionPracticeHistoryService {

    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionTypeRepository questionTypeRepository;
    private final AnswerRepository answerRepository;
    private final DrillAnswerRepository drillAnswerRepository;
    private final SessionRepository sessionRepository;
    private final PresignedUrlService presignedUrlService;

    @Transactional(readOnly = true)
    public QuestionPracticeHistoryResponse getQuestionPracticeHistory(Long questionId, Long userId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        Category category = categoryRepository.findById(question.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        QuestionType questionType = questionTypeRepository.findById(question.getQuestionTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Question type not found"));

        // 세션 기반 답변 조회
        List<Answer> sessionAnswers = answerRepository.findByQuestionId(questionId).stream()
                .filter(Answer::isUploadSuccess)
                .collect(Collectors.toList());

        List<Long> sessionIds = sessionAnswers.stream()
                .map(Answer::getSessionId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Session> sessionMap = sessionRepository.findAllById(sessionIds).stream()
                .filter(s -> s.getUserId().equals(userId))
                .collect(Collectors.toMap(Session::getId, s -> s));

        List<QuestionPracticeHistoryResponse.SessionAnswerInfo> sessionAnswerInfos = sessionAnswers.stream()
                .filter(a -> sessionMap.containsKey(a.getSessionId()))
                .map(answer -> {
                    Session session = sessionMap.get(answer.getSessionId());
                    return QuestionPracticeHistoryResponse.SessionAnswerInfo.builder()
                            .answer(resolveSessionAnswerResponse(answer))
                            .sessionId(session.getId().toString())
                            .sessionTitle(session.getTitle())
                            .build();
                })
                .collect(Collectors.toList());

        // 드릴 기반 답변 조회
        List<DrillAnswer> drillAnswers = drillAnswerRepository
                .findByUserIdAndQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
                        userId, questionId, UploadStatus.SUCCESS.getCode());

        List<DrillAnswerResponse> drillAnswerResponses = drillAnswers.stream()
                .map(this::resolveDrillAnswerResponse)
                .collect(Collectors.toList());

        // 통계
        QuestionPracticeHistoryResponse.PracticeStatistics statistics =
                QuestionPracticeHistoryResponse.PracticeStatistics.builder()
                        .sessionPracticeCount(sessionAnswerInfos.size())
                        .drillPracticeCount(drillAnswerResponses.size())
                        .totalPracticeCount(sessionAnswerInfos.size() + drillAnswerResponses.size())
                        .build();

        return QuestionPracticeHistoryResponse.builder()
                .question(QuestionMapper.toResponse(question))
                .category(CategoryMapper.toResponse(category))
                .questionType(QuestionTypeMapper.toResponse(questionType))
                .sessionAnswers(sessionAnswerInfos)
                .drillAnswers(drillAnswerResponses)
                .statistics(statistics)
                .build();
    }

    private AnswerResponse resolveSessionAnswerResponse(Answer answer) {
        String resolvedAudioUrl = resolveAudioUrl(answer.getAudioUrl(), answer.getStorageType());
        return AnswerMapper.toResponse(answer, resolvedAudioUrl);
    }

    private DrillAnswerResponse resolveDrillAnswerResponse(DrillAnswer answer) {
        String resolvedAudioUrl = resolveAudioUrl(answer.getAudioUrl(), answer.getStorageType());
        return DrillAnswerMapper.toResponse(answer, resolvedAudioUrl);
    }

    private String resolveAudioUrl(String audioUrl, StorageType storageType) {
        if (storageType == StorageType.S3) {
            return presignedUrlService.generateDownloadUrl(audioUrl).getUploadUrl();
        }
        return audioUrl;
    }
}
