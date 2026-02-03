package me.thinkcat.opic.practice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    PENDING("SES0001", "pending"),
    IN_PROGRESS("SES0002", "in_progress"),
    PAUSED("SES0003", "paused"),
    COMPLETED("SES0004", "completed"),
    CANCELLED("SES0005", "cancelled");

    private final String code;
    private final String text;

    public static SessionStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid session status code: " + code));
    }
}
