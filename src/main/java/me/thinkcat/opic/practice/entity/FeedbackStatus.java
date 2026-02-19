package me.thinkcat.opic.practice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * AI 피드백 생성 상태
 * 코드 체계: FBS + 4자리 숫자 (status_code_convention.md 참고)
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackStatus {

    /** 피드백 생성 전 (권한 없는 사용자 포함) */
    NONE("FBS0001", "none"),

    /** 피드백 생성 요청됨 (Lambda invoke 완료, 결과 대기 중) */
    REQUESTED("FBS0002", "requested"),

    /** 피드백 생성 완료 */
    COMPLETED("FBS0003", "completed"),

    /** 피드백 생성 실패 (Lambda 오류, 타임아웃 등 시스템 원인) */
    FAILED("FBS0004", "failed"),

    /** 비정상 녹음 감지 (비영어, 너무 짧은 답변 등 사용자 원인) */
    INVALID("FBS0005", "invalid");

    private final String code;
    private final String text;

    public static FeedbackStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid feedback status code: " + code));
    }
}
