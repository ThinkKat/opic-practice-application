package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {

    @NotBlank(message = "Mode is required")
    private String mode;

    @NotEmpty(message = "At least one category is required")
    private List<Long> categoryIds;

    @NotNull(message = "Question count is required")
    @Positive(message = "Question count must be positive")
    private Integer questionCount;

    private String title;
}
