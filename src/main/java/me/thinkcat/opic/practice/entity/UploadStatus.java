package me.thinkcat.opic.practice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 답변 오디오 파일의 업로드 상태
 * 코드 체계: UPL + 4자리 숫자 (status_code_convention.md 참고)
 */
@Getter
@RequiredArgsConstructor
public enum UploadStatus {

    /** 업로드 대기 중 (Presigned URL 발급됨, 파일 미업로드) */
    PENDING("UPL0001", "pending"),

    /** 업로드 완료 (S3에 파일 존재 확인됨) */
    SUCCESS("UPL0002", "success"),

    /** 업로드 실패 (타임아웃 또는 명시적 실패) */
    FAILED("UPL0003", "failed");

    private final String code;
    private final String text;

    public static UploadStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid upload status code: " + code));
    }
}
