package com.studydocs.manager.service;

import com.studydocs.manager.entity.AuditLog;
import com.studydocs.manager.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long actorId,
                    Long targetUserId,
                    String action,
                    String details,
                    String ip,
                    String userAgent) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setTargetUserId(targetUserId);
        log.setAction(action);
        log.setDetails(details);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        auditLogRepository.save(log);
    }
}

