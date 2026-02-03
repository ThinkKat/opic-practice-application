package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.AnswerMapper;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;
    private final PresignedUrlService presignedUrlService;

    @Transactional
    public AnswerResponse createAnswer(
            Long userId,
            Long sessionId,
            Long questionId,
            MultipartFile audioFile,
            Integer durationMs) {

        // 세션 권한 확인
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        // 파일 저장
        String directory = "answers/" + sessionId;
        String audioUrl = fileStorageService.storeFile(audioFile, directory);

        // Answer 생성
        Answer answer = Answer.builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .audioUrl(audioUrl)
                .storageType(StorageType.LOCAL)
                .mimeType(audioFile.getContentType())
                .durationMs(durationMs != null ? durationMs : 0)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        return resolveAnswerResponse(savedAnswer);
    }

    @Transactional
    public AnswerResponse completeAnswerUpload(
            Long userId,
            Long sessionId,
            Long questionId,
            String fileKey,
            String mimeType,
            Integer durationMs) {

        // 세션 권한 확인
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        // Answer 생성 (S3 업로드 완료 후)
        Answer answer = Answer.builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .audioUrl(fileKey)
                .storageType(StorageType.S3)
                .mimeType(mimeType)
                .durationMs(durationMs != null ? durationMs : 0)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        return resolveAnswerResponse(savedAnswer);
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> getSessionAnswers(Long sessionId, Long userId) {
        // 세션 권한 확인
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        return answers.stream()
                .map(this::resolveAnswerResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnswerResponse getAnswerById(Long answerId, Long userId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        // 세션을 통한 권한 확인
        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        return resolveAnswerResponse(answer);
    }

    @Transactional
    public void deleteAnswer(Long answerId, Long userId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        // 세션을 통한 권한 확인
        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        // 파일 삭제
        fileStorageService.deleteFile(answer.getAudioUrl());

        // Soft Delete
        answer.softDelete();
        answerRepository.save(answer);
    }

    /**
     * Answer의 audioUrl을 실제 HTTP URL로 변환
     * - LOCAL: 백엔드 스트리밍 엔드포인트 URL
     * - S3: Presigned URL (다운로드용)
     */
    private String resolveAudioUrl(Answer answer) {
        if (answer.getStorageType() == StorageType.S3) {
            // S3: Presigned URL 생성
            return presignedUrlService.generateDownloadUrl(answer.getAudioUrl())
                    .getUploadUrl();
        } else {
            // LOCAL: 백엔드 스트리밍 엔드포인트 (상대 경로)
            return "/api/v1/files/stream/" + answer.getAudioUrl();
        }
    }

    /**
     * AnswerResponse의 audioUrl을 HTTP URL로 변환
     */
    private AnswerResponse resolveAnswerResponse(Answer answer) {
        AnswerResponse response = AnswerMapper.toResponse(answer);
        // audioUrl을 HTTP URL로 변환
        return AnswerResponse.builder()
                .id(response.getId())
                .questionId(response.getQuestionId())
                .sessionId(response.getSessionId())
                .audioUrl(resolveAudioUrl(answer))
                .storageType(response.getStorageType())
                .mimeType(response.getMimeType())
                .durationMs(response.getDurationMs())
                .transcript(response.getTranscript())
                .createdAt(response.getCreatedAt())
                .build();
    }
}
