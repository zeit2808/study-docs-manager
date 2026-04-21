package com.studydocs.manager.repository;

import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTop1ByUserAndUsedFalseOrderByCreatedAtDesc(User user);
    Optional<PasswordResetToken> findTop1ByUserAndUsedFalseAndExpiredAtAfter(
            User user, LocalDateTime now);
}
