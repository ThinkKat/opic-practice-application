package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.config.security.JwtTokenProvider;
import me.thinkcat.opic.practice.dto.request.UserRegisterRequest;
import me.thinkcat.opic.practice.dto.response.UserResponse;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private UserService userService;

    @ParameterizedTest
    @ValueSource(strings = {
            "password1@",   // 대문자 없음
            "PASSWORD1@",   // 소문자 없음
            "Password@",    // 숫자 없음
            "Password1",    // 특수문자 없음
            "Pa1@"          // 8자 미만
    })
    void 유효하지않은_비밀번호로_회원가입시_ValidationException(String password) {
        given(userRepository.existsByUsername(any())).willReturn(false);

        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest("user", password, "a@b.com", LocalDateTime.now(), LocalDateTime.now())))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void 유효한_비밀번호와_이메일로_회원가입시_성공() {
        User saved = User.builder().id(1L).username("user").build();
        given(userRepository.existsByUsername("user")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded");
        given(userRepository.save(any(User.class))).willReturn(saved);

        UserResponse response = userService.register(
                new UserRegisterRequest("user", "Password1@", "user@example.com", LocalDateTime.now(), LocalDateTime.now()));

        assertThat(response.getUsername()).isEqualTo("user");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "notanemail",
            "@nodomain.com",
            "missing@",
            "missing@dot"
    })
    void 유효하지않은_이메일로_회원가입시_ValidationException(String email) {
        given(userRepository.existsByUsername(any())).willReturn(false);

        assertThatThrownBy(() ->
                userService.register(new UserRegisterRequest("user", "Password1@", email, LocalDateTime.now(), LocalDateTime.now())))
                .isInstanceOf(ValidationException.class);
    }
}
