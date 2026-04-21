package com.studydocs.manager.repository;

import com.studydocs.manager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Update failed login attempts và lock time trực tiếp vào DB
     * Dùng native query để đảm bảo tên cột chính xác
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users SET failed_login_attempts = :attempts, locked_until = :lockedUntil WHERE username = :username", nativeQuery = true)
    int updateFailedLoginAttempts(@Param("username") String username,
            @Param("attempts") Integer attempts,
            @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * Reset failed login attempts trực tiếp vào DB
     * Dùng native query để đảm bảo tên cột chính xác
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = :userId", nativeQuery = true)
    int resetFailedLoginAttempts(@Param("userId") Long userId);
}
