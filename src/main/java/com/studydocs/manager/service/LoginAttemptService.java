package com.studydocs.manager.service;

import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service quản lý failed login attempts và lockout policy bằng Redis.
 * 
 * Ưu điểm:
 * - Không bị ảnh hưởng bởi transaction rollback
 * - Nhanh hơn database (in-memory)
 * - Có TTL tự động (expire sau X phút)
 * - Phù hợp distributed system (shared state)
 * 
 * Strategy:
 * - Redis key: "login:attempts:{username}" -> số lần sai
 * - Redis key: "login:locked:{username}" -> thời gian unlock (timestamp)
 */
@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

    private static final String ATTEMPTS_KEY_PREFIX = "login:attempts:";
    private static final String LOCKED_KEY_PREFIX = "login:locked:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int ATTEMPTS_TTL_HOURS = 24; // Giữ attempts trong 24h

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Tăng số lần login sai cho user.
     * @return true nếu đạt ngưỡng và bị lock, false nếu chưa
     */
    public boolean incrementFailedAttempts(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
        
        // Tăng counter trong Redis (atomic operation)
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        
        // Set TTL cho key (24 giờ)
        redisTemplate.expire(attemptsKey, ATTEMPTS_TTL_HOURS, TimeUnit.HOURS);
        
        // Nếu đạt ngưỡng, lock account
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            lockAccount(username);
            return true; // Đã bị lock
        }
        
        return false; // Chưa bị lock
    }

    /**
     * Lock account trong Redis với TTL.
     * Đồng thời sync ngay vào Database để đảm bảo persistence.
     */
    public void lockAccount(String username) {
        String lockedKey = LOCKED_KEY_PREFIX + username;
        LocalDateTime unlockTime = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        
        // Lưu timestamp unlock vào Redis với TTL = LOCK_MINUTES
        redisTemplate.opsForValue().set(lockedKey, unlockTime.toString(), 
                                        LOCK_MINUTES, TimeUnit.MINUTES);
        
        // Sync ngay vào Database (async để không block)
        syncLockStatusToDatabase(username, MAX_ATTEMPTS, unlockTime);
        
        logger.warn("Account locked due to multiple failed login attempts - username: {}, unlockTime: {}", 
                   username, unlockTime);
    }

    /**
     * Kiểm tra xem account có bị lock không.
     * @return LocalDateTime unlock time nếu bị lock, null nếu không
     */
    public LocalDateTime isAccountLocked(String username) {
        String lockedKey = LOCKED_KEY_PREFIX + username;
        String unlockTimeStr = redisTemplate.opsForValue().get(lockedKey);
        
        if (unlockTimeStr != null) {
            try {
                LocalDateTime unlockTime = LocalDateTime.parse(unlockTimeStr);
                // Nếu chưa hết thời gian lock
                if (unlockTime.isAfter(LocalDateTime.now())) {
                    return unlockTime;
                } else {
                    // Đã hết thời gian lock, xóa key
                    redisTemplate.delete(lockedKey);
                    return null;
                }
            } catch (Exception e) {
                // Nếu parse lỗi, xóa key và return null
                redisTemplate.delete(lockedKey);
                return null;
            }
        }
        
        return null; // Không bị lock
    }

    /**
     * Reset failed attempts khi login thành công.
     * Đồng thời sync vào Database.
     */
    public void resetFailedAttempts(String username) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
        String lockedKey = LOCKED_KEY_PREFIX + username;
        
        // Xóa cả attempts và locked keys trong Redis
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockedKey);
        
        // Sync reset vào Database
        syncLockStatusToDatabase(username, 0, null);
        
        logger.info("Reset failed login attempts - username: {}", username);
    }

    /**
     * Lấy số lần login sai hiện tại (để logging/debugging).
     */
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

    /**
     * Sync lock status từ Redis vào Database.
     * Được gọi ngay khi lock/unlock account và định kỳ bởi scheduled task.
     */
    @Transactional
    public void syncLockStatusToDatabase(String username, int failedAttempts, LocalDateTime lockedUntil) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user != null) {
                user.setFailedLoginAttempts(failedAttempts);
                user.setLockedUntil(lockedUntil);
                userRepository.save(user);
                
                // Chỉ log khi sync lock status (quan trọng) hoặc khi debug enabled
                if (failedAttempts >= MAX_ATTEMPTS || logger.isDebugEnabled()) {
                    logger.debug("Synced lock status to DB - username: {}, failedAttempts: {}, lockedUntil: {}", 
                               username, failedAttempts, lockedUntil);
                }
            }
        } catch (Exception e) {
            logger.error("Error syncing lock status to database for user: {}", username, e);
            // Không throw exception để không ảnh hưởng đến Redis operations
        }
    }

    /**
     * Background job: Sync tất cả failed attempts và lock status từ Redis vào Database.
     * Chạy mỗi 5 phút để đảm bảo data được persistent.
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300,000 milliseconds
    @Transactional
    public void syncAllToDatabase() {
        try {
            logger.debug("Starting scheduled sync from Redis to Database...");
            
            // Lấy tất cả keys liên quan đến login attempts
            Set<String> attemptKeys = redisTemplate.keys(ATTEMPTS_KEY_PREFIX + "*");
            Set<String> lockedKeys = redisTemplate.keys(LOCKED_KEY_PREFIX + "*");
            
            int syncedCount = 0;
            
            // Sync failed attempts
            if (attemptKeys != null) {
                for (String key : attemptKeys) {
                    String username = key.substring(ATTEMPTS_KEY_PREFIX.length());
                    String attemptsStr = redisTemplate.opsForValue().get(key);
                    
                    if (attemptsStr != null) {
                        try {
                            int attempts = Integer.parseInt(attemptsStr);
                            
                            // Lấy lock status nếu có
                            String lockedKey = LOCKED_KEY_PREFIX + username;
                            String unlockTimeStr = redisTemplate.opsForValue().get(lockedKey);
                            LocalDateTime lockedUntil = null;
                            
                            if (unlockTimeStr != null) {
                                try {
                                    lockedUntil = LocalDateTime.parse(unlockTimeStr);
                                    // Nếu đã hết thời gian lock, set null
                                    if (lockedUntil.isBefore(LocalDateTime.now())) {
                                        lockedUntil = null;
                                    }
                                } catch (Exception e) {
                                    // Ignore parse error
                                }
                            }
                            
                            syncLockStatusToDatabase(username, attempts, lockedUntil);
                            syncedCount++;
                        } catch (NumberFormatException e) {
                            // Ignore invalid attempts value
                        }
                    }
                }
            }
            
            // Sync locked accounts (nếu chưa có trong attempts)
            if (lockedKeys != null) {
                for (String key : lockedKeys) {
                    String username = key.substring(LOCKED_KEY_PREFIX.length());
                    String unlockTimeStr = redisTemplate.opsForValue().get(key);
                    
                    if (unlockTimeStr != null) {
                        try {
                            LocalDateTime lockedUntil = LocalDateTime.parse(unlockTimeStr);
                            
                            // Chỉ sync nếu chưa có trong attempts list
                            String attemptsKey = ATTEMPTS_KEY_PREFIX + username;
                            if (!redisTemplate.hasKey(attemptsKey)) {
                                // Nếu đã hết thời gian lock, set null
                                if (lockedUntil.isBefore(LocalDateTime.now())) {
                                    lockedUntil = null;
                                }
                                syncLockStatusToDatabase(username, 0, lockedUntil);
                                syncedCount++;
                            }
                        } catch (Exception e) {
                            // Ignore parse error
                        }
                    }
                }
            }
            
            if (syncedCount > 0) {
                logger.info("Scheduled sync completed. Synced {} users from Redis to Database.", syncedCount);
            } else {
                logger.debug("Scheduled sync completed. No users to sync.");
            }
        } catch (Exception e) {
            logger.error("Error in scheduled sync from Redis to Database", e);
        }
    }
}

