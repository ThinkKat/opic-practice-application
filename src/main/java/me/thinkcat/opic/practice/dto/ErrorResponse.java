package me.thinkcat.opic.practice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private String errorCode;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
