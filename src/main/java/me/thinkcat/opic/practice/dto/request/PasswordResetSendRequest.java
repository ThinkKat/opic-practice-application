package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PasswordResetSendRequest {

    @NotNull
    private UUID resetSessionId;

    @NotBlank
    @Email
    private String email;
}
