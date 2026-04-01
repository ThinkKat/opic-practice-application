package me.thinkcat.opic.practice.service;

import me.thinkcat.opic.practice.entity.PasswordResetSession;
import me.thinkcat.opic.practice.entity.User;

import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.PasswordResetSessionRepository;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;


@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetSessionRepository passwordResetSessionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordService, "resetCodeSecret", "test-secret");
        ReflectionTestUtils.setField(passwordService, "sessionExpireMinutes", 30);
        ReflectionTestUtils.setField(passwordService, "codeExpireMinutes", 5);
        ReflectionTestUtils.setField(passwordService, "maxAttempts", 5);
        ReflectionTestUtils.setField(passwordService, "resendCooldownSeconds", 60);
        ReflectionTestUtils.setField(passwordService, "maxResendCount", 3);
        ReflectionTestUtils.setField(passwordService, "blockMinutes", 60);
    }

    // Tests for startResetPasswordSession()

    @Test
    void 세션_생성_후_UUID_반환() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        BDDMockito.given(passwordResetSessionRepository.save(ArgumentMatchers.any())).willReturn(session);

        // when
        UUID result = passwordService.startResetPasswordSession();

        // then
        Mockito.verify(passwordResetSessionRepository).save(ArgumentMatchers.any());
        Assertions.assertThat(result).isEqualTo(session.getId());
    }

    @Test
    void 정상_이메일_전송() throws Exception {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        User user = User.builder().id(1L).email("test@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        BDDMockito.given(passwordResetSessionRepository.findTopByUserIdAndBlockedUntilIsNotNullOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());

        // when
        passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");

        // then
        Mockito.verify(emailService).sendPasswordResetCode(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.anyInt());
    }

    @Test
    void 이메일_전송_세션_없음() {
        // given
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());

        // when & then
        Assertions.assertThatThrownBy(() -> {
                    passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 이메일_전송_세션_만료() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        ReflectionTestUtils.setField(passwordService, "sessionExpireMinutes", 0); // Testing for session expiration.
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any()))
                .willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() -> {
            passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");
        }).isInstanceOf(ValidationException.class).hasMessage("Session expired");
    }

    @Test
    void 이전_세션_블록으로_인해_이메일_전송_실패() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        PasswordResetSession blockedPrevSession = new PasswordResetSession();
        session.setCreatedAt();
        blockedPrevSession.setCreatedAt();
        blockedPrevSession.block(10);
        User user = User.builder().id(1L).email("test@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        BDDMockito.given(passwordResetSessionRepository.findTopByUserIdAndBlockedUntilIsNotNullOrderByCreatedAtDesc(user.getId())).willReturn(Optional.of(blockedPrevSession));

        // when & then
        Assertions.assertThatThrownBy(() -> {
            passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");
        }).isInstanceOf(ValidationException.class).hasMessage("Too many resend attempts");
    }

    @Test
    void 현재_세션_블록으로_인해_이메일_전송_실패() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.block(10);
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any()))
                .willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() -> {
            passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");
        }).isInstanceOf(ValidationException.class).hasMessage("Too many resend attempts");
    }

    @Test
    void 세션_쿨다운_상태_재전송_실패() {
        // given
        String codeHash = "codeHash";
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() -> {
            passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");
        }).isInstanceOf(ValidationException.class).hasMessage("Please wait before resending");
    }

    @Test
    void 이메일_미존재_유저() throws NoSuchAlgorithmException, InvalidKeyException {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

        // when
        passwordService.sendResetTokenEmail(UUID.randomUUID(), "test@test.com");

        // then
        Mockito.verify(emailService, Mockito.never()).sendPasswordResetCode(ArgumentMatchers.any(), ArgumentMatchers.any(),ArgumentMatchers.anyInt());
    }

    @Test
    void 다른_userId를_가지는_이메일로_전송_요청() throws NoSuchAlgorithmException, InvalidKeyException {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        long userId = 1L;
        long anotherUserId = 2L;
        String email = "test@test.com";
        String anotherEmail = "anotherTest@test.com";
        ReflectionTestUtils.setField(passwordService, "resendCooldownSeconds", 0); // removing cooldown for testing
        User user = User.builder().id(userId).email(email).build();
        User anotherUser = User.builder().id(anotherUserId).email(anotherEmail).build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        BDDMockito.given(userRepository.findByEmail(anotherEmail)).willReturn(Optional.of(anotherUser));

        passwordService.sendResetTokenEmail(session.getId(), email);

        // when & then
        Assertions.assertThatThrownBy(() -> passwordService.sendResetTokenEmail(session.getId(), anotherEmail)).isInstanceOf(ValidationException.class)
                .hasMessage("Invalid session");
    }

    @Test
    void resedCount_maxResendCount_도달() throws NoSuchAlgorithmException, InvalidKeyException {
        // given
        ReflectionTestUtils.setField(passwordService, "resendCooldownSeconds", 0); // removing cooldown for testing
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        for (int i = 0; i < 4; ++i) {
            session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        }
        long userId = 1L;
        String email = "test@test.com";
        User user = User.builder().id(userId).email(email).build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        passwordService.sendResetTokenEmail(session.getId(), user.getEmail());

        // then
        Assertions.assertThat(session.isBlocked()).isEqualTo(true);
    }

    // Tests for verifyResetToken()

    @Test
    void 정상_코드_검증() throws Exception {
        // given
        String token = "123456";
        String codeHash = hashForTest(token);
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        User user = User.builder().id(1L).email("test@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        // when & then (예외 없음)
        passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", token);
    }

    @Test
    void 코드_검증_세션_없음() {
        // given
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", "123456"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 코드_검증_세션_만료() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        ReflectionTestUtils.setField(passwordService, "sessionExpireMinutes", 0);
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Session expired");
    }

    @Test
    void 코드_검증_코드_만료() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().minusMinutes(1));
        session.assignUser(1L);
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Code expired");
    }

    @Test
    void 코드_검증_시도_횟수_초과() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        for (int i = 0; i < 5; i++) {
            session.verify("wrong");
        }
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Too many attempts");
    }

    @Test
    void 코드_검증_userId_불일치() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        User anotherUser = User.builder().id(2L).email("another@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("another@test.com")).willReturn(Optional.of(anotherUser));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "another@test.com", "123456"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid session");
    }

    @Test
    void 코드_검증_잘못된_코드() throws Exception {
        // given
        String token = "123456";
        String codeHash = hashForTest(token);
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send(codeHash, LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        User user = User.builder().id(1L).email("test@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.verifyResetToken(UUID.randomUUID(), "test@test.com", "wrong"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid code");
    }

    // Tests for resetPassword()

    @Test
    void 정상_비밀번호_변경() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        session.verify("codeHash"); // verifiedAt 설정
        User user = User.builder().id(1L).email("test@test.com").password("oldPassword").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        BDDMockito.given(passwordEncoder.encode(ArgumentMatchers.any())).willReturn("encodedNewPassword");

        // when
        passwordService.resetPassword(UUID.randomUUID(), "test@test.com", "newPassword1@");

        // then
        Mockito.verify(refreshTokenRepository).revokeAllByUserId(ArgumentMatchers.eq(1L), ArgumentMatchers.any());
        Assertions.assertThat(session.isUsed()).isTrue();
    }

    @Test
    void 비밀번호_변경_세션_없음() {
        // given
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.empty());

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.resetPassword(UUID.randomUUID(), "test@test.com", "newPassword1@"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void 비밀번호_변경_세션_만료() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        ReflectionTestUtils.setField(passwordService, "sessionExpireMinutes", 0);
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.resetPassword(UUID.randomUUID(), "test@test.com", "newPassword1@"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Session expired");
    }

    @Test
    void 비밀번호_변경_코드_미검증() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.resetPassword(UUID.randomUUID(), "test@test.com", "newPassword1@"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Code not verified");
    }

    @Test
    void 비밀번호_변경_세션_이미_사용됨() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        session.verify("codeHash");
        session.use();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.resetPassword(UUID.randomUUID(), "test@test.com", "newPassword1@"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Session already used");
    }

    @Test
    void 비밀번호_변경_userId_불일치() {
        // given
        PasswordResetSession session = new PasswordResetSession();
        session.setCreatedAt();
        session.send("codeHash", LocalDateTime.now().plusMinutes(5));
        session.assignUser(1L);
        session.verify("codeHash");
        User anotherUser = User.builder().id(2L).email("another@test.com").build();
        BDDMockito.given(passwordResetSessionRepository.findById(ArgumentMatchers.any())).willReturn(Optional.of(session));
        BDDMockito.given(userRepository.findByEmail("another@test.com")).willReturn(Optional.of(anotherUser));

        // when & then
        Assertions.assertThatThrownBy(() ->
                passwordService.resetPassword(UUID.randomUUID(), "another@test.com", "newPassword1@"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid session");
    }

    // helpers
    // [TODO] PasswordService.hashCode() replication. If PasswordService.hashCode() changes, this must be updated accordingly.
    private String hashForTest(String code) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
    }
}
