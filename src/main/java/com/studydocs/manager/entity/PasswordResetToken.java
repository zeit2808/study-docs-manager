package com.studydocs.manager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_prt_user_expiry", columnList = "user_id, expiry_time"),
        @Index(name = "idx_prt_expiry_time", columnList = "expiry_time")
})
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // BCrypt hash, tuyệt đối không phải OTP gốc.
    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(User user, String otpHash, LocalDateTime expiredAt) {
        this.user = user;
        this.otpHash = otpHash;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        attemptCount++;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}