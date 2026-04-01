package me.thinkcat.opic.practice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_session")
@NoArgsConstructor
@Getter
public class PasswordResetSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "expires_at")
    private LocalDateTime codeExpiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // Count attempt without changing reset code
    @Column(name = "attempt_count")
    private int attemptCount;

    // Count resend email for changing or re-create reset token
    @Column(name = "resend_count")
    private int resendCount;

    @Column(name = "last_resent_at")
    private LocalDateTime lastResentAt;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;


    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isSessionExpired(int n) {
        return LocalDateTime.now().isAfter(createdAt.plusMinutes(n));
    }

    public boolean isBlocked() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    public boolean isCoolDown(int n) {
        return lastResentAt != null && LocalDateTime.now().isBefore(lastResentAt.plusSeconds(n));
    }

    public boolean isCodeExpired() {
        return codeExpiresAt != null && LocalDateTime.now().isAfter(codeExpiresAt);
    }

    public boolean isExhausted(int maxAttempts) {
        return attemptCount >= maxAttempts;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void assignUser(Long userId) {
        this.userId = userId;
    }

    public void send(String codeHash, LocalDateTime codeExpiresAt) {
        this.codeHash = codeHash;
        this.codeExpiresAt = codeExpiresAt;
        this.attemptCount = 0; // Reset attempt
        ++this.resendCount;
        this.lastResentAt = LocalDateTime.now();
    }

    public void block(int blockMinutes) {
        this.blockedUntil = LocalDateTime.now().plusMinutes(blockMinutes);
    }

    public boolean verify(String hashedToken) {
        if (hashedToken.equals(codeHash)) {
            this.verifiedAt = LocalDateTime.now();
            return true;
        }
        ++this.attemptCount;
        return false;
    }

    // Check usage this session for changing password
    // There might be a case that a user doesn't use session after verifying a token.
    public void use() {
        this.usedAt = LocalDateTime.now();
    }
}
