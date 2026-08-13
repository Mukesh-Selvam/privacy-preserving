package com.hackathon.gateway.controller;

import com.hackathon.gateway.dto.ApiResponse;
import com.hackathon.gateway.entity.AuditLog;
import com.hackathon.gateway.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Immutable audit trail endpoint.
 *
 * <p>Returns paginated audit log entries, newest first. Pagination is critical
 * for enterprise deployments where thousands of entries accumulate quickly.
 */
@RestController
@RequestMapping("/api/audit-log")
@Tag(name = "Audit Trail", description = "Immutable record of all data access events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
        summary = "Get paginated audit log",
        description = """
            Returns a paginated, newest-first list of all data access events.
            Each entry records who accessed what, which org requested it,
            which fields were masked or removed, and the decision timestamp.
            """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> recent(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of entries per page (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 100);
        Page<AuditLog> entries = auditService.recentPaged(
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "timestamp")));
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }
}
