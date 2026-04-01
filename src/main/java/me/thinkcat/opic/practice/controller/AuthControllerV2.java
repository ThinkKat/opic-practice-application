package me.thinkcat.opic.practice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.request.LoginEmailRequest;
import me.thinkcat.opic.practice.dto.request.PasswordResetRequest;
import me.thinkcat.opic.practice.dto.request.PasswordResetSendRequest;
import me.thinkcat.opic.practice.dto.request.PasswordResetVerifyRequest;
import me.thinkcat.opic.practice.dto.request.UserRegisterWithoutRequest;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.service.PasswordService;
import me.thinkcat.opic.practice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthControllerV2 {

    private final UserService userService;
    private final PasswordService passwordService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<UserResponse>> register(@Valid @RequestBody UserRegisterWithoutRequest request) {
        UserResponse userResponse = userService.registerWithoutUsername(request);

        CommonResponse<UserResponse> response = CommonResponse.<UserResponse>builder()
                .success(true)
                .result(userResponse)
                .message("User registered successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<TokenResponse>> login(@Valid @RequestBody LoginEmailRequest request,
                                                               HttpServletRequest httpRequest) {
        httpRequest.setAttribute("auth-username", request.getEmail());
        TokenResponse tokenResponse = userService.loginByEmail(request.getEmail(), request.getPassword());

        CommonResponse<TokenResponse> response = CommonResponse.<TokenResponse>builder()
                .success(true)
                .result(tokenResponse)
                .message("Login successful")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset-start")
    public ResponseEntity<CommonResponse<UUID>> passwordResetStart() {
        UUID resetSessionId = passwordService.startResetPasswordSession();

        CommonResponse<UUID> response = CommonResponse.<UUID>builder()
                .success(true)
                .result(resetSessionId)
                .message("Password reset session started")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset-send")
    public ResponseEntity<CommonResponse<Void>> passwordResetSend(@Valid @RequestBody PasswordResetSendRequest request)
            throws NoSuchAlgorithmException, InvalidKeyException {
        passwordService.sendResetTokenEmail(request.getResetSessionId(), request.getEmail());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Code sent")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset-verify")
    public ResponseEntity<CommonResponse<Void>> passwordResetVerify(@Valid @RequestBody PasswordResetVerifyRequest request)
            throws NoSuchAlgorithmException, InvalidKeyException {
        passwordService.verifyResetToken(request.getResetSessionId(), request.getEmail(), request.getCode());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Code verified")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<CommonResponse<Void>> passwordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordService.resetPassword(request.getResetSessionId(), request.getEmail(), request.getNewPassword());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Password reset successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
