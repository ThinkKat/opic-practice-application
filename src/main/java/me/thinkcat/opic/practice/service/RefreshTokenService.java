package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.entity.RefreshToken;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.TokenExpiredException;
import me.thinkcat.opic.practice.exception.UnauthorizedException;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import me.thinkcat.opic.practice.config.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String token = jwtTokenProvider.generateRefreshToken(user.getUsername(), user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityInMillis())))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public TokenResponse refreshTokens(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> {
                    log.warn("event=token_rotation_fail | reason=invalid_token");
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            log.warn("event=token_rotation_fail | who={} | reason=expired",
                    refreshToken.getUser().getUsername());
            throw new TokenExpiredException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        // Rotation: 기존 삭제 → 새로 발급
        refreshTokenRepository.delete(refreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId(), user.getUserRole());
        RefreshToken newRefreshToken = createRefreshToken(user);

        log.info("event=token_rotation_success | who={}", user.getUsername());
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                .build();
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    log.info("event=token_revoke | who={}", token.getUser().getUsername());
                    refreshTokenRepository.delete(token);
                });
    }

    @Transactional
    public void revokeAllByUser(User user) {
        log.info("event=token_revoke_all | who={}", user.getUsername());
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
