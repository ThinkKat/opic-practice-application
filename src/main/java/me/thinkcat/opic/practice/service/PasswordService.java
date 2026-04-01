package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.entity.PasswordResetSession;
import me.thinkcat.opic.practice.entity.User;
import me.thinkcat.opic.practice.exception.ResourceNotFoundException;
import me.thinkcat.opic.practice.exception.ValidationException;
import me.thinkcat.opic.practice.repository.PasswordResetSessionRepository;
import me.thinkcat.opic.practice.repository.RefreshTokenRepository;
import me.thinkcat.opic.practice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    final private UserRepository userRepository;
    final private PasswordResetSessionRepository passwordResetSessionRepository;
    final private RefreshTokenRepository refreshTokenRepository;
    final private PasswordEncoder passwordEncoder;
    final private EmailService emailService;
    final private String hashAlgorithm = "HmacSHA256";

    @Value("${password.reset.code.secret}")
    private String resetCodeSecret;

    @Value("${password.reset.session-expire-minutes}")
    private int sessionExpireMinutes;

    @Value("${password.reset.code-expire-minutes}")
    private int codeExpireMinutes;

    @Value("${password.reset.max-attempts}")
    private int maxAttempts;

    @Value("${password.reset.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${password.reset.max-resend-count}")
    private int maxResendCount;

    @Value("${password.reset.block-minutes}")
    private int blockMinutes;


    // Start resetting password session
    @Transactional
    public UUID startResetPasswordSession() {
        PasswordResetSession session = new PasswordResetSession();
        session = passwordResetSessionRepository.save(session);
        return session.getId();
    }

    // Invoke async function (abstraction) for sending a reset token email
    @Transactional
    public void sendResetTokenEmail(UUID sessionId, String email) throws NoSuchAlgorithmException, InvalidKeyException {
        PasswordResetSession session = passwordResetSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No session found"));

        // Validate which session can resend
        if (session.isSessionExpired(sessionExpireMinutes)) throw new ValidationException("Session expired");
        if (session.isBlocked()) throw new ValidationException("Too many resend attempts");
        if (session.isCoolDown(resendCooldownSeconds)) throw new ValidationException("Please wait before resending");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("event=password_reset_email_skip | reason=user_not_found | email={}", email);
            return;
        }
        User user = userOpt.get();

        // Check valid blocked sessions ard existed
        passwordResetSessionRepository.findTopByUserIdAndBlockedUntilIsNotNullOrderByCreatedAtDesc(user.getId())
                .ifPresent(prev -> {
                    if (prev.isBlocked()) throw new ValidationException("Too many resend attempts");
                });

        // Create code
        String token = generateResetToken();
        String hashedCode = hashCode(token);
        if (session.getUserId() == null) {
            session.assignUser(user.getId());
        } else if (!session.getUserId().equals(user.getId())) {
            throw new ValidationException("Invalid session"); // Validate user - session
        }
        session.send(hashedCode, LocalDateTime.now().plusMinutes(codeExpireMinutes));
        if (session.getResendCount() >= maxResendCount) {
            session.block(blockMinutes);
        }

        log.info("event=password_reset_email_invoke | email={}", email);
        emailService.sendPasswordResetCode(email, token, codeExpireMinutes);
    }

    // Verify reset token
    @Transactional
    public void verifyResetToken(UUID sessionId, String email, String token) throws NoSuchAlgorithmException, InvalidKeyException {
        PasswordResetSession session = passwordResetSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No session found"));

        // Check which token can be verified (attempt)
        if (session.isSessionExpired(sessionExpireMinutes)) throw new ValidationException("Session expired");
        if (session.isCodeExpired()) throw new ValidationException("Code expired");
        if (session.isExhausted(maxAttempts)) throw new ValidationException("Too many attempts");

        // Validate User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("Invalid request"));
        if (session.getUserId() == null || !session.getUserId().equals(user.getId())) throw new ValidationException("Invalid session");

        String hashedToken = hashCode(token);
        if (!session.verify(hashedToken)) {
            throw new ValidationException("Invalid code");
        }
    }

    // Reset Password
    @Transactional
    public void resetPassword(UUID sessionId, String email, String newPassword) {
        // Find session
        PasswordResetSession session = passwordResetSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No session found"));

        // Validate session state
        if (session.isSessionExpired(sessionExpireMinutes)) throw new ValidationException("Session expired");
        if (!session.isVerified()) throw new ValidationException("Code not verified");
        if (session.isUsed()) throw new ValidationException("Session already used");

        // Validate email + sessionId + userId
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("Invalid request"));
        if (session.getUserId() == null || !session.getUserId().equals(user.getId())) {
            throw new ValidationException("Invalid session");
        }

        // Encode new password and update
        user.setPassword(passwordEncoder.encode(newPassword));

        // Revoke all existing refresh tokens
        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        // Mark session as used
        session.use();
    }

    // Create token
    private String generateResetToken() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    // HMAC_SHA256 해싱
    private String hashCode(String code) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(hashAlgorithm);
        SecretKeySpec keySpec = new SecretKeySpec(resetCodeSecret.getBytes(StandardCharsets.UTF_8), hashAlgorithm);
        mac.init(keySpec);
        return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
    }
}
