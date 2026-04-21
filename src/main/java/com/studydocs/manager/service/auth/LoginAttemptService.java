package com.studydocs.manager.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final String ATTEMPTS_KEY_PREFIX = "login:attempts:";
    private static final String LOCKED_KEY_PREFIX = "login:locked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int ATTEMPTS_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final LoginAttemptPersistenceService loginAttemptPersistenceService;

    public LoginAttemptService(
            StringRedisTemplate redisTemplate,
            LoginAttemptPersistenceService loginAttemptPersistenceService) {
        this.redisTemplate = redisTemplate;
        this.loginAttemptPersistenceService = loginAttemptPersistenceService;
    }

    public boolean incrementFailedAttempts(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        redisTemplate.expire(attemptsKey, ATTEMPTS_TTL_HOURS, TimeUnit.HOURS);

        int currentAttempts = attempts != null ? attempts.intValue() : 0;
        loginAttemptPersistenceService.syncLockStatusToDatabase(username, currentAttempts, null);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            lockAccount(username, currentAttempts);
            return true;
        }

        return false;
    }

    public void lockAccount(String username, int failedAttempts) {
        String lockedKey = LOCKED_KEY_PREFIX + username;
        LocalDateTime unlockTime = LocalDateTime.now().plusMinutes(LOCK_MINUTES);

        redisTemplate.opsForValue().set(lockedKey, unlockTime.toString(), LOCK_MINUTES, TimeUnit.MINUTES);
        loginAttemptPersistenceService.syncLockStatusToDatabase(username, failedAttempts, unlockTime);

        logger.warn("Account locked due to multiple failed login attempts - username: {}, unlockTime: {}",
                username, unlockTime);
    }

    public LocalDateTime isAccountLocked(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
        String lockedKey = LOCKED_KEY_PREFIX + username;
        String unlockTimeStr = redisTemplate.opsForValue().get(lockedKey);

        if (unlockTimeStr != null) {
            try {
                LocalDateTime unlockTime = LocalDateTime.parse(unlockTimeStr);
                if (unlockTime.isAfter(LocalDateTime.now())) {
                    return unlockTime;
                }

                redisTemplate.delete(lockedKey);
                redisTemplate.delete(attemptsKey);
                loginAttemptPersistenceService.syncLockStatusToDatabase(username, 0, null);
                return null;
            } catch (Exception e) {
                redisTemplate.delete(lockedKey);
                redisTemplate.delete(attemptsKey);
                loginAttemptPersistenceService.syncLockStatusToDatabase(username, 0, null);
                return null;
            }
        }

        return null;
    }

    public void resetFailedAttempts(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
        String lockedKey = LOCKED_KEY_PREFIX + username;

        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockedKey);
        loginAttemptPersistenceService.syncLockStatusToDatabase(username, 0, null);

        logger.info("Reset failed login attempts - username: {}", username);
    }

    public int getFailedAttempts(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);

        if (attemptsStr != null) {
            try {
                return Integer.parseInt(attemptsStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }
}
