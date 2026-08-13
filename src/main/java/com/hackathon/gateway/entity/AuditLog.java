package com.hackathon.gateway.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "org_id")
    private String orgId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "api_called")
    private String apiCalled;

    @Column(name = "policy_applied")
    private String policyApplied;

    @Column(name = "fields_masked")
    private String fieldsMasked;

    @Column(name = "fields_removed")
    private String fieldsRemoved;

    private String status;

    @Column(name = "ip_address")
    private String ipAddress;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getApiCalled() { return apiCalled; }
    public void setApiCalled(String apiCalled) { this.apiCalled = apiCalled; }

    public String getPolicyApplied() { return policyApplied; }
    public void setPolicyApplied(String policyApplied) { this.policyApplied = policyApplied; }

    public String getFieldsMasked() { return fieldsMasked; }
    public void setFieldsMasked(String fieldsMasked) { this.fieldsMasked = fieldsMasked; }

    public String getFieldsRemoved() { return fieldsRemoved; }
    public void setFieldsRemoved(String fieldsRemoved) { this.fieldsRemoved = fieldsRemoved; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
