package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.entity.RefreshToken;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.TokenExpiredException;
import me.thinkcat.opic.practice.exception.UnauthorizedException;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import me.thinkcat.opic.practice.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

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
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenExpiredException("Refresh token expired");
        }

        User user = refreshToken.getUser();

        // Rotation: 기존 삭제 → 새로 발급
        refreshTokenRepository.delete(refreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getId());
        RefreshToken newRefreshToken = createRefreshToken(user);

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
                .ifPresent(refreshTokenRepository::delete);
    }
}
