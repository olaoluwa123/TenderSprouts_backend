package com.example.backend.service;

import com.example.backend.entity.AuditLog;
import com.example.backend.entity.User;
import com.example.backend.repository.AuditLogRepository;
import com.example.backend.security.SecurityUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAdminAction(SecurityUser actor, String action, String entityType, Long entityId, String details) {
        if (actor.getRole() != User.Role.ADMIN && actor.getRole() != User.Role.TEACHER) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setUserId(actor.getUserId());
        log.setUserRole(actor.getRole().name());
        if (actor.getRole() == User.Role.ADMIN) {
            log.setAdminId(actor.getProfileId());
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
