package me.thinkcat.opic.practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Terms agreement timestamp is required")
    private LocalDateTime termsAgreedAt;

    @NotNull(message = "Privacy agreement timestamp is required")
    private LocalDateTime privacyAgreedAt;
}
