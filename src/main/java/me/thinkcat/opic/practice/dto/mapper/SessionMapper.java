package me.thinkcat.opic.practice.dto.mapper;

import me.thinkcat.opic.practice.dto.response.SessionResponse;
import me.thinkcat.opic.practice.entity.Session;
import me.thinkcat.opic.practice.entity.SessionStatus;

public class SessionMapper {

    public static SessionResponse toResponse(Session session) {
        SessionStatus status = session.getStatus();

        return SessionResponse.builder()
                .id(session.getId() != null ? session.getId().toString() : null)
                .userId(session.getUserId() != null ? session.getUserId().toString() : null)
                .questionSetId(session.getQuestionSetId() != null ? session.getQuestionSetId().toString() : null)
                .title(session.getTitle())
                .mode(session.getMode())
                .status(status.name())
                .statusText(status.getText())
                .currentIndex(session.getCurrentIndex())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
