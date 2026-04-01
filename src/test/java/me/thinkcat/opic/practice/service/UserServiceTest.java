package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.config.security.JwtTokenProvider;
import me.thinkcat.opic.practice.dto.request.UserRegisterRequest;
import me.thinkcat.opic.practice.dto.request.UserRegisterWithoutRequest;
import me.thinkcat.opic.practice.dto.response.TokenResponse;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.entity.RefreshToken;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.UserRepository;
import me.thinkcat.opic.practice.validation.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Spy private UserValidator userValidator;
    @InjectMocks private UserService userService;

    // ── v1 register() 테스트 ──────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "password1@",   // 대문자 없음
            "PASSWORD1@",   // 소문자 없음
            "Password@",    // 숫자 없음
            "Password1",    // 특수문자 없음
            "Pa1@"          // 8자 미만
    })
    void 유효하지않은_비밀번호로_회원가입시_ValidationException(String password) {
        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest("testuser", "a@b.com", password, LocalDateTime.now(), LocalDateTime.now())))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void 유효한_비밀번호와_이메일로_회원가입시_성공() {
        // given
        String username = "testuser";
        String email = "user@example.com";
        String password = "Password1@";
        User saved = User.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();

        given(userRepository.existsByUsername(username)).willReturn(false);
        given(userRepository.existsByEmail(email)).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn(password);
        given(userRepository.save(any(User.class))).willReturn(saved);

        // when
        UserResponse response = userService.register(
            new UserRegisterRequest(username, email, password, LocalDateTime.now(), LocalDateTime.now())
        );

        // then
        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 이메일_중복시_예외() {
        // given
        String email = "user@example.com";
        String password = "Password1@";

        given(userRepository.existsByUsername(any())).willReturn(false);
        given(userRepository.existsByEmail(any())).willReturn(true);

        // when
        Exception exception = assertThrows(ValidationException.class, () ->
            userService.register(new UserRegisterRequest("testuser", email, password, LocalDateTime.now(), LocalDateTime.now()))
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("Email already exists");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "notanemail",
            "@nodomain.com",
            "missing@",
            "missing@dot"
    })
    void 유효하지않은_이메일로_회원가입시_ValidationException(String email) {
        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest("testuser", email, "Password1@", LocalDateTime.now(), LocalDateTime.now())))
                .isInstanceOf(ValidationException.class);
    }

    // ── v2 registerWithAutoUsername() 테스트 ─────────────────────────────

    @Test
    void username_랜덤생성_prefix_형식_확인() {
        // given
        String email = "user@example.com";
        String password = "Password1@";

        given(userRepository.existsByUsername(any())).willReturn(false);
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn(password);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        given(userRepository.save(captor.capture())).willReturn(
                User.builder()
                    .username("user_abc123")
                    .id(1L)
                    .email(email)
                    .build()
        );

        // when
        userService.registerWithoutUsername(
                new UserRegisterWithoutRequest(email, password, LocalDateTime.now(), LocalDateTime.now())
        );

        // then
        String username = captor.getValue().getUsername();
        assertThat(username).matches("user_[a-z0-9]{6}");
    }

    @Test
    void username_충돌_재시도_후_성공() {
        // given
        String email = "user@example.com";
        String password = "Password1@";
        User saved = User.builder()
                .username("user_abc123")
                .id(1L)
                .email(email)
                .build();
        given(userRepository.existsByUsername(any()))
                .willReturn(true)
                .willReturn(true)
                .willReturn(false);
        given(userRepository.existsByEmail(any())).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn(password);
        given(userRepository.save(any(User.class))).willReturn(saved);

        // when
        userService.registerWithoutUsername(
                new UserRegisterWithoutRequest(email, password, LocalDateTime.now(), LocalDateTime.now())
        );

        // then
        verify(userRepository, times(3)).existsByUsername(any());
    }

    @Test
    void username_10번_충돌시_예외() {
        // given
        String email = "user@example.com";
        String password = "Password1@";

        given(userRepository.existsByEmail(any())).willReturn(false);
        Boolean[] trues = Collections.nCopies(10, true).toArray(new Boolean[0]);
        given(userRepository.existsByUsername(any()))
                .willReturn(true, trues);

        // when
        Exception exception = assertThrows(ValidationException.class, () ->
                userService.registerWithoutUsername(new UserRegisterWithoutRequest(email, password, LocalDateTime.now(), LocalDateTime.now()))
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("Failed to generate unique username. Please try again.");
    }

    // ── v2 loginByEmail() 테스트 ─────────────────────────────────────────

    @Test
    void 동일_사용자_연속_두번_로그인시_서로다른_refreshToken_반환() {
        // given
        String email = "user@example.com";
        User user = User.builder().id(1L).email(email).username("user").build();
        RefreshToken firstToken = RefreshToken.builder().token("refresh-token-aaa").build();
        RefreshToken secondToken = RefreshToken.builder().token("refresh-token-bbb").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(any(), any(), any())).willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenValidityInSeconds()).willReturn(3600L);
        given(refreshTokenService.createRefreshToken(user))
                .willReturn(firstToken)
                .willReturn(secondToken);

        TokenResponse response1 = userService.loginByEmail(email, "Password1@");
        TokenResponse response2 = userService.loginByEmail(email, "Password1@");

        assertThat(response1.getRefreshToken()).isNotEqualTo(response2.getRefreshToken());
    }
}
