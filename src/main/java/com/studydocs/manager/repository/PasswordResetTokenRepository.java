package com.studydocs.manager.repository;

import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken>
    findTop1ByUserAndExpiredAtAfterOrderByCreatedAtDesc(
            User user,
            LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken>
    findTop1ByUserOrderByCreatedAtDesc(User user);

    long deleteByUser(User user);
}