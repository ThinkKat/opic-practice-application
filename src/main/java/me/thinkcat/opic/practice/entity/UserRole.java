package me.thinkcat.opic.practice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 사용자 권한 등급
 * 코드 체계: USR + 4자리 숫자 (status_code_convention.md 참고)
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    /** 무료 사용자 - 피드백 생성 불가 */
    FREE("USR0001", "free"),

    /** 유료 사용자 - 피드백 생성 가능 */
    PAID("USR0002", "paid"),

    /** 관리자 - 모든 기능 사용 가능 */
    ADMIN("USR0003", "admin");

    private final String code;
    private final String text;

    public static UserRole fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid user role code: " + code));
    }
}
