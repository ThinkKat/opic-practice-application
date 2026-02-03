package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.mapper.SessionMapper;
import me.thinkcat.opic.practice.dto.request.SessionCreateRequest;
import me.thinkcat.opic.practice.dto.response.SessionResponse;
import me.thinkcat.opic.practice.entity.Question;
import me.thinkcat.opic.practice.entity.QuestionSet;
import me.thinkcat.opic.practice.entity.QuestionSetItem;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.SessionStatus;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.QuestionSetItemRepository;
import me.thinkcat.opic.practice.repository.QuestionSetRepository;
import me.thinkcat.opic.practice.repository.SessionRepository;
import me.thinkcat.opic.practice.service.question.QuestionSelector;
import me.thinkcat.opic.practice.service.question.policy.NoOpSelectionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetItemRepository questionSetItemRepository;
    private final QuestionSelector questionSelector;
    private final NoOpSelectionPolicy defaultPolicy;

    @Transactional
    public SessionResponse createSession(Long userId, SessionCreateRequest request) {
        // 1. 정책을 사용해서 질문 선택
        List<Question> selectedQuestions = questionSelector.selectQuestions(
                request.getCategoryIds(),
                request.getQuestionCount(),
                defaultPolicy
        );

        // 2. QuestionSet 생성
        String title = request.getTitle() != null ? request.getTitle() :
                "Session " + LocalDateTime.now();

        QuestionSet questionSet = QuestionSet.builder()
                .userId(userId)
                .title(title)
                .build();
        questionSet = questionSetRepository.save(questionSet);

        // 3. QuestionSetItem 생성
        for (int i = 0; i < selectedQuestions.size(); i++) {
            QuestionSetItem item = QuestionSetItem.builder()
                    .questionSetId(questionSet.getId())
                    .questionId(selectedQuestions.get(i).getId())
                    .orderIndex(i)
                    .build();
            questionSetItemRepository.save(item);
        }

        // 4. Session 생성
        Session session = Session.builder()
                .userId(userId)
                .questionSetId(questionSet.getId())
                .title(title)
                .mode(request.getMode())
                .statusCode(SessionStatus.PENDING.getCode())
                .currentIndex(0)
                .build();

        Session savedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(savedSession);
    }

    @Transactional
    public SessionResponse startSession(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);

        if (!session.isPending()) {
            throw new ValidationException("Only pending sessions can be started");
        }

        session.start();
        Session updatedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(updatedSession);
    }

    @Transactional
    public SessionResponse pauseSession(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);

        if (!session.isInProgress()) {
            throw new ValidationException("Only in-progress sessions can be paused");
        }

        session.pause();
        Session updatedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(updatedSession);
    }

    @Transactional
    public SessionResponse resumeSession(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);

        if (!session.isPaused()) {
            throw new ValidationException("Only paused sessions can be resumed");
        }

        session.resume();
        Session updatedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(updatedSession);
    }

    @Transactional
    public SessionResponse completeSession(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);

        if (!session.isInProgress()) {
            throw new ValidationException("Only in-progress sessions can be completed");
        }

        session.complete();
        Session updatedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(updatedSession);
    }

    @Transactional
    public SessionResponse cancelSession(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);

        if (session.isCompleted() || session.isCancelled()) {
            throw new ValidationException("Cannot cancel completed or already cancelled sessions");
        }

        session.cancel();
        Session updatedSession = sessionRepository.save(session);

        return SessionMapper.toResponse(updatedSession);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(Long sessionId, Long userId) {
        Session session = findSessionByIdAndUserId(sessionId, userId);
        return SessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getUserSessions(Long userId) {
        List<Session> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream()
                .map(SessionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Session findSessionByIdAndUserId(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
    }
}
