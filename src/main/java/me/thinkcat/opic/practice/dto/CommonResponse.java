package me.thinkcat.opic.practice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommonResponse<T> {
    private boolean success;
    private T result;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
