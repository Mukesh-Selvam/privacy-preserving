package com.hackathon.gateway.controller;

import com.hackathon.gateway.dto.ApiResponse;
import com.hackathon.gateway.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Core gateway endpoint: retrieves patient data subject to dual-gating by
 * org-level OPA policy and patient consent.
 */
@RestController
@RequestMapping("/api/gateway")
@Tag(name = "Data Gateway", description = "Policy and consent-gated patient data access")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Operation(
        summary = "Fetch patient record with policy and consent enforcement",
        description = """
            Retrieves a patient's data record filtered through two independent gates:
            1. **Organisation OPA policy** — governs what fields the org may access at all.
            2. **Patient consent** — governs what fields the patient has agreed to share.
            
            Fields are returned as **plain**, **FPE-encrypted**, or **"— removed —"** depending on combined decisions.
            Every call is immutably written to the audit log.
            
            Example: `GET /api/gateway/patient/1?orgId=insurer-partner&userId=claims-agent-001`
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Record returned (some fields may be hidden or encrypted)",
            content = @Content(examples = @ExampleObject(
                value = """
                    {"name":"Rahul","age":"35","disease":"Diabetes","aadhaar":"781495260378","phone":"— removed —","address":"— removed —"}
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Missing or invalid X-API-Key header"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Patient not found")
    })
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProtectedPatientData(
            @Parameter(description = "Numeric patient identifier", example = "1")
            @PathVariable Integer patientId,

            @Parameter(description = "Requesting organisation ID matching OPA policy rules",
                       example = "insurer-partner")
            @RequestParam String orgId,

            @Parameter(description = "Caller user/agent identifier for the audit log",
                       example = "claims-agent-001", required = false)
            @RequestParam(required = false) String userId,

            HttpServletRequest request) {

        String resolvedUserId = userId;
        if (resolvedUserId == null || resolvedUserId.trim().isEmpty()) {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            resolvedUserId = (auth != null && auth.getName() != null) ? auth.getName() : "api-client";
        }

        Map<String, Object> record = gatewayService.fetchProtectedRecord(
                patientId, orgId, resolvedUserId, request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok(record));
    }
}
