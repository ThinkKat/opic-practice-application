package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.mapper.AnswerMapper;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.FeedbackFailureReason;
import me.thinkcat.opic.practice.entity.FeedbackStatus;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.entity.UserRole;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {

    private final FeatureFlagService featureFlagService;

    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final PresignedUrlService presignedUrlService;
    private final FeedbackLambdaService feedbackLambdaService;

    @Transactional
    public PrepareAnswerUploadResponse prepareAnswerUpload(
            Long userId,
            Long sessionId,
            Long questionId,
            String fileName,
            String contentType,
            Long contentLength) {

        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        String fileKey = generateFileKey(sessionId, questionId, fileName);

        PresignedUrlRequest request = new PresignedUrlRequest(fileKey, contentType, contentLength);
        PresignedUrlResponse presignedUrlResponse = presignedUrlService.generateUploadUrl(request);

        Answer answer = Answer.builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .audioUrl(fileKey)
                .storageType(StorageType.S3)
                .mimeType(contentType)
                .durationMs(0)
                .build();

        Answer savedAnswer = answerRepository.save(answer);
        log.info("event=answer_prepare | who={} | sessionId={} | questionId={} | answerId={}",
                userId, sessionId, questionId, savedAnswer.getId());

        return PrepareAnswerUploadResponse.builder()
                .answerId(savedAnswer.getId() != null ? savedAnswer.getId().toString() : null)
                .uploadUrl(presignedUrlResponse.getUploadUrl())
                .fileKey(fileKey)
                .expiresAt(presignedUrlResponse.getExpiresAt())
                .requiredHeaders(presignedUrlResponse.getRequiredHeaders())
                .build();
    }

    @Transactional
    public AnswerResponse completeAnswerUpload(
            Long userId,
            UserRole userRole,
            Long answerId,
            Integer durationMs) {

        Answer answer = answerRepository.findByIdForUpdate(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        if (answer.isUploadPending()) {
            answer.markUploadSuccess();
            log.info("event=answer_upload_complete | who={} | answerId={} | audioUrl={}",
                    userId, answerId, answer.getAudioUrl());
        }

        if (durationMs != null && answer.getDurationMs() == 0) {
            answer.setDurationMs(durationMs);
        }

        if (!answer.isFeedbackNone()) {
            log.warn("event=answer_feedback_already_requested | who={} | answerId={}", userId, answerId);
            return resolveAnswerResponse(answerRepository.save(answer));
        }

        if (userRole == UserRole.PAID || userRole == UserRole.ADMIN || featureFlagService.isEnabled("ai-for-free")) {
            answer.requestFeedback();
            Answer updatedAnswer = answerRepository.save(answer);
            log.info("event=feedback_requested | who={} | answerId={} | audioUrl={}",
                    userId, answerId, answer.getAudioUrl());
            String questionText = questionRepository.findById(answer.getQuestionId())
                    .map(Question::getQuestion)
                    .orElse(null);
            feedbackLambdaService.invokeSessionFeedbackAsync(answer.getAudioUrl(), userId, questionText);
            return resolveAnswerResponse(updatedAnswer);
        }

        Answer updatedAnswer = answerRepository.save(answer);
        return resolveAnswerResponse(updatedAnswer);
    }

    @Transactional
    public void handleS3UploadDetected(String fileKey) {
        Answer answer = answerRepository.findByAudioUrlForUpdate(fileKey).orElse(null);
        if (answer == null) {
            log.warn("event=s3_upload_detected_not_found | fileKey={}", fileKey);
            return;
        }

        if (answer.isUploadPending()) {
            answer.markUploadSuccess();
            log.info("event=s3_upload_detected_marked_success | answerId={} | audioUrl={}",
                    answer.getId(), fileKey);
        }

        if (!answer.isFeedbackNone()) {
            log.warn("event=s3_upload_detected_feedback_already_requested | answerId={} | audioUrl={}",
                    answer.getId(), fileKey);
            answerRepository.save(answer);
            return;
        }

        Long userId = sessionRepository.findById(answer.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found for answer: " + answer.getId()))
                .getUserId();

        UserRole userRole = userRepository.findById(userId)
                .map(User::getUserRole)
                .orElse(UserRole.FREE);

        if (userRole == UserRole.PAID || userRole == UserRole.ADMIN || featureFlagService.isEnabled("ai-for-free")) {
            answer.requestFeedback();
            answerRepository.save(answer);
            log.info("event=s3_upload_detected_feedback_requested | who={} | answerId={} | audioUrl={}",
                    userId, answer.getId(), fileKey);
            String questionText = questionRepository.findById(answer.getQuestionId())
                    .map(Question::getQuestion)
                    .orElse(null);
            feedbackLambdaService.invokeSessionFeedbackAsync(fileKey, userId, questionText);
            return;
        }

        answerRepository.save(answer);
        log.info("event=s3_upload_detected_no_feedback | who={} | answerId={}", userId, answer.getId());
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> getSessionAnswers(Long sessionId, Long userId) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        return answers.stream()
                .filter(answer -> answer.isUploadSuccess())
                .map(this::resolveAnswerResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnswerResponse getAnswerForPlayback(Long answerId, Long userId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        if (answer.isUploadPending() && answer.getStorageType() == StorageType.S3) {

            boolean fileExists = presignedUrlService.checkFileExists(answer.getAudioUrl());

            if (fileExists) {
                log.warn("event=answer_upload_recovery | who={} | answerId={} | audioUrl={}",
                        userId, answerId, answer.getAudioUrl());
                answer.markUploadSuccess();
                answerRepository.save(answer);
            } else {
                long minutesSinceCreation = Duration.between(
                        answer.getCreatedAt(),
                        LocalDateTime.now()
                ).toMinutes();

                if (minutesSinceCreation > 30) {
                    log.warn("event=answer_upload_failed | who={} | answerId={} | audioUrl={}",
                            userId, answerId, answer.getAudioUrl());
                    answer.markUploadFailed();
                    answerRepository.save(answer);
                    throw new ValidationException("Answer upload failed or timed out");
                }

                throw new ValidationException("Answer is still being uploaded. Please try again later.");
            }
        }

        if (answer.isUploadFailed()) {
            throw new ValidationException("Answer upload failed");
        }

        return resolveAnswerResponse(answer);
    }

    @Transactional
    public void updateTranscription(String audioUrl, String transcription,
                                     String wordSegments, String pauseAnalysis,
                                     Double duration) {
        Answer answer = answerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with audioUrl: " + audioUrl));

        answer.setTranscript(transcription);
        if (wordSegments != null) answer.setWordSegments(wordSegments);
        if (pauseAnalysis != null) answer.setPauseAnalysis(pauseAnalysis);
        if (duration != null && answer.getDurationMs() == 0) {
            answer.setDurationMs((int) (duration * 1000));
        }
        answerRepository.save(answer);
        log.info("event=feedback_transcription_saved | audioUrl={}", audioUrl);
    }

    @Transactional
    public void updateFeedback(String audioUrl, String feedback) {
        Answer answer = answerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with audioUrl: " + audioUrl));

        answer.setFeedback(feedback);
        answer.completeFeedback();
        answerRepository.save(answer);
        log.info("event=feedback_completed | answerId={} | audioUrl={}", answer.getId(), audioUrl);
    }

    @Transactional
    public void updateFeedbackFailed(String audioUrl, FeedbackFailureReason reason) {
        Answer answer = answerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with audioUrl: " + audioUrl));

        boolean isInvalid = reason == FeedbackFailureReason.NON_ENGLISH
                || reason == FeedbackFailureReason.TOO_SHORT;
        if (isInvalid) {
            answer.invalidateFeedback();
            log.warn("event=feedback_failed_callback | answerId={} | audioUrl={} | reason={}",
                    answer.getId(), audioUrl, reason.getCode());
        } else {
            answer.failFeedback();
            log.error("event=feedback_error_callback | answerId={} | audioUrl={} | reason={}",
                    answer.getId(), audioUrl, reason.getCode());
        }
        answerRepository.save(answer);
    }

    private AnswerResponse resolveAnswerResponse(Answer answer) {
        AnswerResponse response = AnswerMapper.toResponse(answer);
        return AnswerResponse.builder()
                .id(response.getId())
                .questionId(response.getQuestionId())
                .sessionId(response.getSessionId())
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

    private String resolveAudioUrl(Answer answer) {
        if (answer.getStorageType() == StorageType.S3) {
            return presignedUrlService.generateDownloadUrl(answer.getAudioUrl())
                    .getUploadUrl();
        } else {
            // TODO: LOCAL 타입은 삭제 예정.
            return "/api/v1/files/stream/" + answer.getAudioUrl();
        }
    }

    /**
     * S3 Object Key 생성
     * 형식: uploads/sessions/{sessionId}/questions/{questionId}/{uuid}.{ext}
     */
    private String generateFileKey(Long sessionId, Long questionId, String fileName) {
        String extension = getFileExtension(fileName);
        String uuid = UUID.randomUUID().toString();
        return String.format("uploads/sessions/%d/questions/%d/%s%s",
                sessionId, questionId, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
