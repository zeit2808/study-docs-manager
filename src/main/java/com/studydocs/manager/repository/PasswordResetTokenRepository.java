package com.studydocs.manager.repository;

import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTop1ByUserAndExpiredAtAfterOrderByCreatedAtDesc(
            User user, LocalDateTime now);

    Optional<PasswordResetToken> findTop1ByUserAndOtpOrderByCreatedAtDesc(User user, String otp);

    long deleteByUser(User user);
}
