package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.mapper.DrillAnswerMapper;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareDrillAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.dto.response.RecentDrillQuestionResponse;
import me.thinkcat.opic.practice.entity.*;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.CategoryRepository;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.RecentDrillQuestionProjection;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrillAnswerService {

    private final FeatureFlagService featureFlagService;

    private final DrillAnswerRepository drillAnswerRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PresignedUrlService presignedUrlService;
    private final FeedbackLambdaService feedbackLambdaService;

    @Transactional
    public PrepareDrillAnswerUploadResponse prepareDrillAnswerUpload(
            Long userId,
            Long questionId,
            String fileName,
            String contentType,
            Long contentLength) {

        questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        String fileKey = generateFileKey(userId, questionId, fileName);

        PresignedUrlRequest request = new PresignedUrlRequest(fileKey, contentType, contentLength);
        PresignedUrlResponse presignedUrlResponse = presignedUrlService.generateUploadUrl(request);

        DrillAnswer drillAnswer = DrillAnswer.builder()
                .userId(userId)
                .questionId(questionId)
                .audioUrl(fileKey)
                .storageType(StorageType.S3)
                .mimeType(contentType)
                .durationMs(0)
                .build();

        DrillAnswer savedAnswer = drillAnswerRepository.save(drillAnswer);
        log.info("event=drill_prepare | who={} | questionId={} | drillAnswerId={}",
                userId, questionId, savedAnswer.getId());

        return PrepareDrillAnswerUploadResponse.builder()
                .drillAnswerId(savedAnswer.getId() != null ? savedAnswer.getId().toString() : null)
                .uploadUrl(presignedUrlResponse.getUploadUrl())
                .fileKey(fileKey)
                .expiresAt(presignedUrlResponse.getExpiresAt())
                .requiredHeaders(presignedUrlResponse.getRequiredHeaders())
                .build();
    }

    @Transactional
    public DrillAnswerResponse submitDrillAnswer(Long userId, UserRole userRole, Long drillAnswerId, Integer durationMs) {
        DrillAnswer answer = drillAnswerRepository.findByIdForUpdate(drillAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with id: " + drillAnswerId));

        if (!answer.getUserId().equals(userId)) {
            throw new ValidationException("Unauthorized access to drill answer");
        }

        if (answer.isUploadPending()) {
            answer.markUploadSuccess();
            log.info("event=drill_submit | who={} | drillAnswerId={} | audioUrl={}",
                    userId, drillAnswerId, answer.getAudioUrl());
        }

        if (durationMs != null && answer.getDurationMs() == 0) {
            answer.setDurationMs(durationMs);
        }

        if (!answer.isFeedbackNone()) {
            log.warn("event=drill_feedback_already_requested | who={} | drillAnswerId={}", userId, drillAnswerId);
            return resolveDrillAnswerResponse(drillAnswerRepository.save(answer));
        }

        if (userRole == UserRole.PAID || userRole == UserRole.ADMIN || featureFlagService.isEnabled("ai-for-free")) {
            answer.requestFeedback();
            DrillAnswer updatedAnswer = drillAnswerRepository.save(answer);
            log.info("event=drill_feedback_requested | who={} | drillAnswerId={} | audioUrl={}",
                    userId, drillAnswerId, answer.getAudioUrl());
            String questionText = questionRepository.findById(answer.getQuestionId())
                    .map(Question::getQuestion)
                    .orElse(null);
            feedbackLambdaService.invokeDrillAnswerFeedbackAsync(answer.getAudioUrl(), userId, questionText);
            return resolveDrillAnswerResponse(updatedAnswer);
        }

        DrillAnswer updatedAnswer = drillAnswerRepository.save(answer);
        return resolveDrillAnswerResponse(updatedAnswer);
    }

    @Transactional
    public void handleS3UploadDetected(String fileKey) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrlForUpdate(fileKey).orElse(null);
        if (answer == null) {
            log.warn("event=drill_s3_upload_detected_not_found | fileKey={}", fileKey);
            return;
        }

        if (answer.isUploadPending()) {
            answer.markUploadSuccess();
            log.info("event=drill_s3_upload_detected_marked_success | drillAnswerId={} | audioUrl={}",
                    answer.getId(), fileKey);
        }

        if (!answer.isFeedbackNone()) {
            log.warn("event=drill_s3_upload_detected_feedback_already_requested | drillAnswerId={} | audioUrl={}",
                    answer.getId(), fileKey);
            drillAnswerRepository.save(answer);
            return;
        }

        UserRole userRole = userRepository.findById(answer.getUserId())
                .map(User::getUserRole)
                .orElse(UserRole.FREE);

        if (userRole == UserRole.PAID || userRole == UserRole.ADMIN || featureFlagService.isEnabled("ai-for-free")) {
            answer.requestFeedback();
            drillAnswerRepository.save(answer);
            log.info("event=drill_s3_upload_detected_feedback_requested | who={} | drillAnswerId={} | audioUrl={}",
                    answer.getUserId(), answer.getId(), fileKey);
            String questionText = questionRepository.findById(answer.getQuestionId())
                    .map(Question::getQuestion)
                    .orElse(null);
            feedbackLambdaService.invokeDrillAnswerFeedbackAsync(fileKey, answer.getUserId(), questionText);
            return;
        }

        drillAnswerRepository.save(answer);
        log.info("event=drill_s3_upload_detected_no_feedback | who={} | drillAnswerId={}",
                answer.getUserId(), answer.getId());
    }

    @Transactional(readOnly = true)
    public List<RecentDrillQuestionResponse> getRecentlyPracticedQuestions(Long userId) {
        List<RecentDrillQuestionProjection> projections = drillAnswerRepository
                .findRecentlyPracticedQuestions(userId, UploadStatus.SUCCESS.getCode());

        List<Long> questionIds = projections.stream()
                .map(RecentDrillQuestionProjection::getQuestionId)
                .toList();

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Long> categoryIds = questionMap.values().stream()
                .map(Question::getCategoryId)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return projections.stream()
                .filter(p -> questionMap.containsKey(p.getQuestionId()))
                .map(p -> {
                    Question question = questionMap.get(p.getQuestionId());
                    String categoryName = categoryNameMap.get(question.getCategoryId());
                    return RecentDrillQuestionResponse.builder()
                            .questionId(p.getQuestionId().toString())
                            .questionText(question.getQuestion())
                            .categoryName(categoryName)
                            .lastPracticedAt(p.getLastPracticedAt())
                            .drillPracticeCount(p.getDrillPracticeCount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrillAnswerResponse> getDrillAnswersByQuestion(Long userId, Long questionId) {
        List<DrillAnswer> answers = drillAnswerRepository
                .findByUserIdAndQuestionIdAndUploadStatusCodeOrderByCreatedAtDesc(
                        userId, questionId, UploadStatus.SUCCESS.getCode());

        return answers.stream()
                .map(this::resolveDrillAnswerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateTranscription(String audioUrl, String transcription,
                                     String wordSegments, String pauseAnalysis,
                                     Double duration) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with audioUrl: " + audioUrl));

        answer.setTranscript(transcription);
        if (wordSegments != null) answer.setWordSegments(wordSegments);
        if (pauseAnalysis != null) answer.setPauseAnalysis(pauseAnalysis);
        if (duration != null && answer.getDurationMs() == 0) {
            answer.setDurationMs((int) (duration * 1000));
        }
        drillAnswerRepository.save(answer);
        log.info("event=drill_transcription_saved | audioUrl={}", audioUrl);
    }

    @Transactional
    public void updateFeedback(String audioUrl, String feedback) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with audioUrl: " + audioUrl));

        answer.setFeedback(feedback);
        answer.completeFeedback();
        drillAnswerRepository.save(answer);
        log.info("event=drill_feedback_completed | drillAnswerId={} | audioUrl={}", answer.getId(), audioUrl);
    }

    @Transactional
    public void updateFeedbackFailed(String audioUrl, FeedbackFailureReason reason) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with audioUrl: " + audioUrl));

        boolean isInvalid = reason == FeedbackFailureReason.NON_ENGLISH
                || reason == FeedbackFailureReason.TOO_SHORT;
        if (isInvalid) {
            answer.invalidateFeedback();
            log.warn("event=drill_feedback_failed_callback | drillAnswerId={} | audioUrl={} | reason={}",
                    answer.getId(), audioUrl, reason.getCode());
        } else {
            answer.failFeedback();
            log.error("event=drill_feedback_error_callback | drillAnswerId={} | audioUrl={} | reason={}",
                    answer.getId(), audioUrl, reason.getCode());
        }
        drillAnswerRepository.save(answer);
    }

    @Transactional
    public void retryFeedback(Long answerId, Long userId) {
        DrillAnswer answer = drillAnswerRepository.findByIdForUpdate(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        // Validate the owner of the answer
        if (!answer.getUserId().equals(userId)) {
            throw new ValidationException("Unauthorized access to drill answer");
        }

        if (!answer.isFeedbackFailed()) {
            log.warn("event=failed_retry_feedback | answerId={} | audioUrl={}",
                    answer.getId(), answer.getAudioUrl());
            throw new ValidationException("Feedback Requested is not failed");
        }

        // No need to validate user role because retry is provided only for
        // the paid user or the answers when were created during event(ai-for-free)
        answer.requestFeedback();
        drillAnswerRepository.save(answer);
        log.info("event=drill_retry_feedback_requested | who={} | answerId={} | audioUrl={}",
                userId, answer.getId(), answer.getAudioUrl());
        String questionText = questionRepository.findById(answer.getQuestionId())
                .map(Question::getQuestion)
                .orElse(null);
        feedbackLambdaService.invokeDrillAnswerFeedbackAsync(answer.getAudioUrl(), userId, questionText);
    }

    private DrillAnswerResponse resolveDrillAnswerResponse(DrillAnswer answer) {
        DrillAnswerResponse response = DrillAnswerMapper.toResponse(answer);
        return DrillAnswerResponse.builder()
                .id(response.getId())
                .userId(response.getUserId())
                .questionId(response.getQuestionId())
                .audioUrl(resolveAudioUrl(answer))
                .storageType(response.getStorageType())
                .mimeType(response.getMimeType())
                .durationMs(response.getDurationMs())
                .transcript(response.getTranscript())
                .pauseAnalysis(response.getPauseAnalysis())
                .feedback(response.getFeedback())
                .uploadStatus(response.getUploadStatus())
                .uploadStatusText(response.getUploadStatusText())
                .feedbackStatus(response.getFeedbackStatus())
                .feedbackStatusText(response.getFeedbackStatusText())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    private String resolveAudioUrl(DrillAnswer answer) {
        if (answer.getStorageType() == StorageType.S3) {
            return presignedUrlService.generateDownloadUrl(answer.getAudioUrl())
                    .getUploadUrl();
        }
        return answer.getAudioUrl();
    }

    /**
     * S3 Object Key 생성
     * 형식: uploads/drills/{userId}/questions/{questionId}/{uuid}.{ext}
     */
    private String generateFileKey(Long userId, Long questionId, String fileName) {
        String extension = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        return String.format("uploads/drills/%d/questions/%d/%s%s",
                userId, questionId, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
