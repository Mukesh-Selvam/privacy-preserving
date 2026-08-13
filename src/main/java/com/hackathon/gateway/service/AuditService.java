package com.hackathon.gateway.service;

import com.hackathon.gateway.entity.AuditLog;
import com.hackathon.gateway.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Immutable audit trail service.
 *
 * <p>Every field-level access decision is recorded here for compliance,
 * forensic, and regulatory reporting purposes.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String orgId, String userId, String apiCalled, String policyApplied,
                        String fieldsMasked, String fieldsRemoved, String status, String ipAddress) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setOrgId(orgId);
        entry.setUserId(userId);
        entry.setApiCalled(apiCalled);
        entry.setPolicyApplied(policyApplied);
        entry.setFieldsMasked(fieldsMasked);
        entry.setFieldsRemoved(fieldsRemoved);
        entry.setStatus(status);
        entry.setIpAddress(ipAddress);
        auditLogRepository.save(entry);
        log.info("[Audit] org={}, user={}, api={}, masked=[{}], removed=[{}]",
                orgId, userId, apiCalled, fieldsMasked, fieldsRemoved);
    }

    /** Used by dashboard.html for the live 50-row feed. */
    public List<AuditLog> recent() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    /** Used by the paginated API endpoint. */
    public Page<AuditLog> recentPaged(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
