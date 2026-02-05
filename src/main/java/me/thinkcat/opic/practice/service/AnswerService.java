package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.AnswerMapper;
import me.thinkcat.opic.practice.dto.request.PresignedUrlRequest;
import me.thinkcat.opic.practice.dto.response.AnswerResponse;
import me.thinkcat.opic.practice.dto.response.PrepareAnswerUploadResponse;
import me.thinkcat.opic.practice.dto.response.PresignedUrlResponse;
import me.thinkcat.opic.practice.entity.Answer;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.StorageType;
import me.thinkcat.opic.practice.entity.UploadStatus;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.AnswerRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final SessionRepository sessionRepository;
    private final FileStorageService fileStorageService;
    private final PresignedUrlService presignedUrlService;

    /**
     * 로컬 파일 업로드 (기존 메서드 - 호환성 유지)
     */
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

        // Answer 생성 (로컬 업로드는 즉시 SUCCESS 상태)
        Answer answer = Answer.builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .audioUrl(audioUrl)
                .storageType(StorageType.LOCAL)
                .mimeType(audioFile.getContentType())
                .durationMs(durationMs != null ? durationMs : 0)
                .uploadStatus(UploadStatus.SUCCESS)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        return resolveAnswerResponse(savedAnswer);
    }

    /**
     * 1단계: S3 업로드 준비 (Presigned URL 발급 + DB 레코드 생성)
     */
    @Transactional
    public PrepareAnswerUploadResponse prepareAnswerUpload(
            Long userId,
            Long sessionId,
            Long questionId,
            String fileName,
            String contentType,
            Long contentLength) {

        // 세션 권한 확인
        Session session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        // S3 Object Key 생성 (AnswerService가 책임)
        String fileKey = generateFileKey(sessionId, questionId, fileName);

        // Presigned URL 발급 (생성한 fileKey 전달)
        PresignedUrlRequest request = new PresignedUrlRequest(fileKey, contentType, contentLength);
        PresignedUrlResponse presignedUrlResponse = presignedUrlService.generateUploadUrl(request);

        // DB에 PENDING 상태로 먼저 저장
        Answer answer = Answer.builder()
                .questionId(questionId)
                .sessionId(sessionId)
                .audioUrl(fileKey)  // 미리 생성한 fileKey 저장
                .storageType(StorageType.S3)
                .mimeType(contentType)
                .durationMs(0)  // 아직 알 수 없음
                .uploadStatus(UploadStatus.PENDING)
                .build();

        Answer savedAnswer = answerRepository.save(answer);

        // 응답 생성
        return PrepareAnswerUploadResponse.builder()
                .answerId(savedAnswer.getId())
                .uploadUrl(presignedUrlResponse.getUploadUrl())
                .fileKey(fileKey)
                .expiresAt(presignedUrlResponse.getExpiresAt())
                .requiredHeaders(presignedUrlResponse.getRequiredHeaders())
                .build();
    }

    /**
     * 2단계: S3 업로드 완료 통지 (PENDING -> SUCCESS)
     */
    @Transactional
    public AnswerResponse completeAnswerUpload(
            Long userId,
            Long answerId,
            Integer durationMs) {

        // Answer 조회
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        // 세션 권한 확인
        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        // 상태 확인 (이미 SUCCESS면 중복 요청)
        if (answer.getUploadStatus() == UploadStatus.SUCCESS) {
            return resolveAnswerResponse(answer);
        }

        // 상태 업데이트: PENDING -> SUCCESS
        answer.setUploadStatus(UploadStatus.SUCCESS);
        if (durationMs != null) {
            answer.setDurationMs(durationMs);
        }

        Answer updatedAnswer = answerRepository.save(answer);

        return resolveAnswerResponse(updatedAnswer);
    }

    /**
     * 세션별 답변 목록 조회 (단순 조회)
     * SUCCESS 상태인 답변만 반환
     */
    @Transactional(readOnly = true)
    public List<AnswerResponse> getSessionAnswers(Long sessionId, Long userId) {
        // 세션 권한 확인
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        List<Answer> answers = answerRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // SUCCESS 상태인 답변만 반환
        return answers.stream()
                .filter(answer -> answer.getUploadStatus() == UploadStatus.SUCCESS)
                .map(this::resolveAnswerResponse)
                .collect(Collectors.toList());
    }

    /**
     * 답변 재생을 위한 조회 (즉시 재생 보장)
     * PENDING 상태인 경우 S3 파일 존재 여부를 확인하여 상태 업데이트
     */
    @Transactional
    public AnswerResponse getAnswerForPlayback(Long answerId, Long userId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + answerId));

        // 세션을 통한 권한 확인
        sessionRepository.findByIdAndUserId(answer.getSessionId(), userId)
                .orElseThrow(() -> new ValidationException("Unauthorized access to answer"));

        // PENDING 상태인 경우 실제 S3 파일 존재 여부 확인
        if (answer.getUploadStatus() == UploadStatus.PENDING
                && answer.getStorageType() == StorageType.S3) {

            // S3에 실제 파일이 존재하는지 확인
            boolean fileExists = presignedUrlService.checkFileExists(answer.getAudioUrl());

            if (fileExists) {
                // 파일 존재 -> SUCCESS로 업데이트
                answer.setUploadStatus(UploadStatus.SUCCESS);
                answerRepository.save(answer);
            } else {
                // 일정 시간 경과 후에도 파일 없으면 FAILED로 처리
                long minutesSinceCreation = Duration.between(
                        answer.getCreatedAt(),
                        LocalDateTime.now()
                ).toMinutes();

                if (minutesSinceCreation > 30) {  // 30분 타임아웃
                    answer.setUploadStatus(UploadStatus.FAILED);
                    answerRepository.save(answer);
                    throw new ValidationException("Answer upload failed or timed out");
                }

                // 아직 타임아웃 전이면 PENDING 상태 유지
                throw new ValidationException("Answer is still being uploaded. Please try again later.");
            }
        }

        // FAILED 상태인 경우 예외 발생
        if (answer.getUploadStatus() == UploadStatus.FAILED) {
            throw new ValidationException("Answer upload failed");
        }

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
                .uploadStatus(response.getUploadStatus())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
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
