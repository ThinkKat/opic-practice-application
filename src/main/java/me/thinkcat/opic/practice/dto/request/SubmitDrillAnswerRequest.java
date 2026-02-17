package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitDrillAnswerRequest {

    @NotNull(message = "Drill answer ID is required")
    private Long drillAnswerId;

    private Integer durationMs;
}
