# Production Readiness Plan

This document defines the path to convert `privacy-gateway` from a local demo into a production-grade privacy gateway service.

## 1. Production Architecture

### Core System
- **Gateway service**
  - Spring Boot API that exposes the gateway endpoint(s)
  - Validates incoming requests from partner systems
  - Performs authorization and auditing
  - Delegates policy decisions to OPA
- **OPA policy engine**
  - Manages field-level access rules
  - Evaluates both org policy and patient consent
  - Uses versioned policy bundles
- **PostgreSQL database**
  - Stores patient data, consent records, audit logs, and entity metadata
  - Uses migrations rather than ad hoc SQL seed files
- **Vault**
  - Stores secrets, database credentials, and FPE keys
  - Supports key rotation and audit logging

### Data Flow
1. Partner request arrives at gateway
2. Gateway authenticates the caller and resolves org identity
3. Gateway fetches patient record and consent state
4. Gateway calls OPA with org, field, and consent inputs
5. OPA returns field mode: plain, encrypted, hidden
6. Gateway applies masking/encryption and returns sanitized record
7. Gateway logs audit event with decision details

## 2. Security and Compliance

### Authentication and Authorization
- Replace `userId` query parameter with real auth
- Support OAuth2 / JWT token validation
- Enforce org-scoped access control
- Gate access to audit and consent APIs

### Data Protection
- Enforce TLS on all connections
- Use Vault for all secrets and keys
- Encrypt sensitive fields using FPE or vault-managed encryption
- Mask or hide disallowed fields consistently

### Audit and Compliance
- Persist structured audit records
- Include request metadata: caller, org, fields masked/removed, policy decisions, request IP
- Use immutable audit storage and retention policy
- Add audit search dashboard or export capability

## 3. Real Data Model

### Entities
- `patient`
  - patientId, name, age, disease, aadhaar, phone, address, etc.
- `patient_consent`
  - patientId, fieldName, consentGiven, updatedAt, createdAt, version
- `audit_log`
  - timestamp, orgId, userId, apiCalled, policyApplied, fieldsMasked, fieldsRemoved, status, ipAddress, requestId
- `partner_org`
  - orgId, name, allowedFields, tags, status

### Migration Strategy
- Add Flyway or Liquibase
- Store schema changes in versioned migration files
- Replace manual `sql/seed-data.sql` with migration-based seed and sandbox data

## 4. Production Deployment

### Environment Strategy
- Use environment-specific config: `dev`, `qa`, `prod`
- Prefer environment variables for sensitive values
- Use Spring Boot profiles and externalized config

### Deployment
- Move from Docker Compose to orchestrated deployment
  - Kubernetes, ECS, or managed container service
- Add health checks and readiness probes
- Add logging and metrics

## 5. Engineering Checklist

1. Create `PRODUCTION.md` with design and checklist
2. Add database migration support
3. Externalize configuration by environment
4. Add real auth/authz pipeline
5. Harden Vault and OPA integration
6. Add audit and policy CI tests
7. Remove demo-only dashboard or make it admin-only
8. Add docs and runbook for deployment

## Next Step

### Start with the first production-grade improvement:
- Define the deployment architecture and config strategy in code and documentation.
- Add a checklist for required production tasks.

