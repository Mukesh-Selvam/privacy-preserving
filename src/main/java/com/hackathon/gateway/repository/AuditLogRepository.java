package com.hackathon.gateway.repository;

import com.hackathon.gateway.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    /** Legacy method kept for dashboard.html backward compatibility. */
    List<AuditLog> findTop50ByOrderByTimestampDesc();

    /** Paginated retrieval for API consumers. */
    Page<AuditLog> findAll(Pageable pageable);
}
