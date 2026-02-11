package me.thinkcat.opic.practice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.request.AccessTokenRefreshRequest;
import me.thinkcat.opic.practice.dto.request.LoginRequest;
import me.thinkcat.opic.practice.dto.request.LogoutRequest;
import me.thinkcat.opic.practice.dto.request.UserRegisterRequest;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.service.RefreshTokenService;
import me.thinkcat.opic.practice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse userResponse = userService.register(request);

        CommonResponse<UserResponse> response = CommonResponse.<UserResponse>builder()
                .success(true)
                .result(userResponse)
                .message("User registered successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request);

        CommonResponse<TokenResponse> response = CommonResponse.<TokenResponse>builder()
                .success(true)
                .result(tokenResponse)
                .message("Login successful")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<TokenResponse>> refresh(@Valid @RequestBody AccessTokenRefreshRequest request) {
        TokenResponse tokenResponse = refreshTokenService.refreshTokens(request.getRefreshToken());

        CommonResponse<TokenResponse> response = CommonResponse.<TokenResponse>builder()
                .success(true)
                .result(tokenResponse)
                .message("Token refreshed successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse userResponse = userService.getUserByUsername(authentication.getName());

        CommonResponse<UserResponse> response = CommonResponse.<UserResponse>builder()
                .success(true)
                .result(userResponse)
                .message("User retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .success(true)
                .message("Logout successful")
                .build();

        return ResponseEntity.ok(response);
    }
}
