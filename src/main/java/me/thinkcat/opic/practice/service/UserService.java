package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.mapper.UserMapper;
import me.thinkcat.opic.practice.dto.request.LoginRequest;
import me.thinkcat.opic.practice.dto.request.UserRegisterRequest;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.dto.response.MeResponse;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.entity.RefreshToken;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.UserRepository;
import me.thinkcat.opic.practice.config.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        validatePassword(request.getPassword());
        validateEmail(request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .termsAgreedAt(request.getTermsAgreedAt())
                .privacyAgreedAt(request.getPrivacyAgreedAt())
                .build();

        User savedUser = userRepository.save(user);

        log.info("event=register | who={} | email={}", request.getUsername(), request.getEmail());
        return UserMapper.toResponse(savedUser);
    }

    @Transactional
    public TokenResponse loginByUsername(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), user.getUserRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("event=login_success | who={}", user.getUsername());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                .user(UserMapper.toResponse(user))
                .build();
    }

    @Transactional
    public TokenResponse loginByEmail(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), password)
        );

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), user.getUserRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("event=login_success | who={}", user.getUsername());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                .user(UserMapper.toResponse(user))
                .build();
    }

    /**
     * Login Method without username
     * AuthControllerV2 uses this method
     */
    @Transactional
    public UserResponse registerWithoutUsername(UserRegisterRequest request) {
        validatePassword(request.getPassword());
        validateEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists");
        }

        String username = generateUniqueUsername(request.getEmail());

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .termsAgreedAt(request.getTermsAgreedAt())
                .privacyAgreedAt(request.getPrivacyAgreedAt())
                .build();

        User savedUser = userRepository.save(user);

        log.info("event=register | who={} | email={}", username, request.getEmail());
        return UserMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return MeResponse.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void withdraw(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenService.revokeAllByUser(user);
        user.softDelete();
        userRepository.save(user);
        log.warn("event=withdraw | who={}", username);
    }

    private static final String SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateUniqueUsername(String email) {
        String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (prefix.isEmpty()) prefix = "user";

        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < SUFFIX_LENGTH; i++) {
                suffix.append(SUFFIX_CHARS.charAt(RANDOM.nextInt(SUFFIX_CHARS.length())));
            }
            String candidate = prefix + "_" + suffix;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new ValidationException("Failed to generate unique username. Please try again.");
    }

    private void validatePassword(String password) {
        String pattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(pattern)) {
            throw new ValidationException("Password must be at least 8 characters with uppercase, lowercase, digit, and special character");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Invalid email format");
        }
    }
}
