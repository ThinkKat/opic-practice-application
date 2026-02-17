package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.DrillAnswerMapper;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.DrillAnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareDrillAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.DrillAnswer;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.DrillAnswerRepository;
import me.thinkcat.opic.practice.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrillAnswerService {

    private final DrillAnswerRepository drillAnswerRepository;
    private final QuestionRepository questionRepository;
    private final PresignedUrlService presignedUrlService;

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
                .uploadStatus(UploadStatus.PENDING)
                .build();

        DrillAnswer savedAnswer = drillAnswerRepository.save(drillAnswer);

        return PrepareDrillAnswerUploadResponse.builder()
                .drillAnswerId(savedAnswer.getId() != null ? savedAnswer.getId().toString() : null)
                .uploadUrl(presignedUrlResponse.getUploadUrl())
                .fileKey(fileKey)
                .expiresAt(presignedUrlResponse.getExpiresAt())
                .requiredHeaders(presignedUrlResponse.getRequiredHeaders())
                .build();
    }

    @Transactional
    public DrillAnswerResponse submitDrillAnswer(Long userId, Long drillAnswerId, Integer durationMs) {
        DrillAnswer answer = drillAnswerRepository.findById(drillAnswerId)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with id: " + drillAnswerId));

        if (!answer.getUserId().equals(userId)) {
            throw new ValidationException("Unauthorized access to drill answer");
        }

        if (answer.getUploadStatus() == UploadStatus.SUCCESS) {
            return resolveDrillAnswerResponse(answer);
        }

        answer.setUploadStatus(UploadStatus.SUCCESS);
        if (durationMs != null) {
            answer.setDurationMs(durationMs);
        }

        DrillAnswer updatedAnswer = drillAnswerRepository.save(answer);
        return resolveDrillAnswerResponse(updatedAnswer);
    }

    @Transactional(readOnly = true)
    public List<DrillAnswerResponse> getDrillAnswersByQuestion(Long userId, Long questionId) {
        List<DrillAnswer> answers = drillAnswerRepository
                .findByUserIdAndQuestionIdAndUploadStatusOrderByCreatedAtDesc(
                        userId, questionId, UploadStatus.SUCCESS);

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
    }

    @Transactional
    public void updateFeedback(String audioUrl, String feedback) {
        DrillAnswer answer = drillAnswerRepository.findByAudioUrl(audioUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Drill answer not found with audioUrl: " + audioUrl));

        answer.setFeedback(feedback);
        drillAnswerRepository.save(answer);
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
