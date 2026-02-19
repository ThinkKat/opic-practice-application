package me.thinkcat.opic.practice.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Lambda → WAS 피드백 실패 사유 코드
 * 코드 체계: FBR + 4자리 숫자 (status_code_convention.md 참고)
 * INVALID/FAILED 분류 판단은 WAS에서 수행 (Lambda는 사유만 전달)
 */
@Getter
@RequiredArgsConstructor
public enum FeedbackFailureReason {

    /** 영어가 아님 → INVALID */
    NON_ENGLISH("FBR0001", "non_english"),

    /** 발화 길이가 너무 짧음 → INVALID */
    TOO_SHORT("FBR0002", "too_short"),

    /** 처리 중 오류 → FAILED */
    PROCESSING_ERROR("FBR0003", "processing_error");

    private final String code;
    private final String text;

    @JsonCreator
    public static FeedbackFailureReason fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid feedback failure reason code: " + code));
    }
}
