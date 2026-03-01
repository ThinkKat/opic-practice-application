package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.config.security.JwtTokenProvider;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.entity.RefreshToken;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.TokenExpiredException;
import me.thinkcat.opic.practice.exception.UnauthorizedException;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RefreshToken validToken() {
        return RefreshToken.builder()
                .id(1L).user(user).token("valid-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private RefreshToken expiredToken() {
        return RefreshToken.builder()
                .id(2L).user(user).token("expired-token")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
    }

    private RefreshToken revokedToken() {
        RefreshToken token = RefreshToken.builder()
                .id(3L).user(user).token("revoked-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        token.revoke();
        return token;
    }

    // ── refreshTokens ─────────────────────────────────────────────────────────

    @Test
    void 정상_토큰으로_rotation시_기존_토큰_revoke_후_새_토큰_반환() {
        RefreshToken existing = validToken();
        given(refreshTokenRepository.findByTokenWithLock("valid-token")).willReturn(Optional.of(existing));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(jwtTokenProvider.generateAccessToken(any(), any(), any())).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(any(), any())).willReturn("new-refresh-token");
        given(jwtTokenProvider.getRefreshTokenValidityInMillis()).willReturn(604800000L);
        given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(3600L);

        TokenResponse response = refreshTokenService.refreshTokens("valid-token");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 존재하지_않는_토큰으로_rotation시_UnauthorizedException() {
        given(refreshTokenRepository.findByTokenWithLock("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refreshTokens("unknown"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 이미_revoked된_토큰으로_rotation시_전체_무효화_후_UnauthorizedException() {
        given(refreshTokenRepository.findByTokenWithLock("revoked-token")).willReturn(Optional.of(revokedToken()));

        assertThatThrownBy(() -> refreshTokenService.refreshTokens("revoked-token"))
                .isInstanceOf(UnauthorizedException.class);

        then(refreshTokenRepository).should().revokeAllByUserId(eq(user.getId()), any(LocalDateTime.class));
    }

    @Test
    void 만료된_토큰으로_rotation시_revoke_후_TokenExpiredException() {
        RefreshToken expired = expiredToken();
        given(refreshTokenRepository.findByTokenWithLock("expired-token")).willReturn(Optional.of(expired));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> refreshTokenService.refreshTokens("expired-token"))
                .isInstanceOf(TokenExpiredException.class);

        assertThat(expired.isRevoked()).isTrue();
        assertThat(expired.getRevokedAt()).isNotNull();
    }

    // ── revokeRefreshToken ───────────────────────────────────────────────────

    @Test
    void 토큰_revoke시_soft_delete_처리() {
        RefreshToken token = validToken();
        given(refreshTokenRepository.findByToken("valid-token")).willReturn(Optional.of(token));
        given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revokeRefreshToken("valid-token");

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_토큰_revoke시_아무_작업_없음() {
        given(refreshTokenRepository.findByToken("unknown")).willReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken("unknown");

        then(refreshTokenRepository).should(never()).save(any());
    }

    // ── revokeAllByUser ──────────────────────────────────────────────────────

    @Test
    void 유저_전체_토큰_revoke시_revokeAllByUserId_호출() {
        refreshTokenService.revokeAllByUser(user);

        then(refreshTokenRepository).should().revokeAllByUserId(eq(user.getId()), any(LocalDateTime.class));
    }
}
