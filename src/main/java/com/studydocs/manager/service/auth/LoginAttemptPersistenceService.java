package com.studydocs.manager.service.auth;

import com.studydocs.manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptPersistenceService.class);

    private final UserRepository userRepository;

    public LoginAttemptPersistenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncLockStatusToDatabase(String username, int failedAttempts, LocalDateTime lockedUntil) {
        try {
            int updatedRows = userRepository.updateFailedLoginAttempts(username, failedAttempts, lockedUntil);

            if (updatedRows == 0) {
                logger.warn("Could not sync lock status to DB because username was not found: {}", username);
                return;
            }
            logger.debug("Synced lock status to DB - username: {}, failedAttempts: {}, lockedUntil: {}",
                    username, failedAttempts, lockedUntil);
        } catch (Exception e) {
            logger.error("Error syncing lock status to database for user: {}", username, e);
        }
    }
}
