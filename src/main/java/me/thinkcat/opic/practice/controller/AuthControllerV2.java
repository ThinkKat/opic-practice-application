package me.thinkcat.opic.practice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.CommonResponse;
import me.thinkcat.opic.practice.dto.request.LoginEmailRequest;
import me.thinkcat.opic.practice.dto.request.UserRegisterWithoutRequest;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthControllerV2 {

    private final UserService userService;

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
}
