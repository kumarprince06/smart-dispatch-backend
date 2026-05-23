package com.smartdispatch.audit.service;

import com.smartdispatch.audit.entity.AuditLog;
import com.smartdispatch.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Audit service — logs all important actions.
 * Uses @Async for non-blocking audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String entityType, Long entityId, String action,
                     String previousValue, String newValue, String performedBy) {
        AuditLog audit = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .previousValue(previousValue)
                .newValue(newValue)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(audit);
        log.debug("Audit: {} {} on {}:{} by {}", action, newValue, entityType, entityId, performedBy);
    }

    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
    }

    public Page<AuditLog> getEntityAudit(String entityType, Long entityId, int page, int size) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }
}
