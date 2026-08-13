-- Production database indexes for sub-millisecond query performance

CREATE INDEX IF NOT EXISTS idx_patient_consent_lookup 
ON patient_consent(patient_id, field_name);

CREATE INDEX IF NOT EXISTS idx_org_policies_lookup 
ON org_policies(org_id, field_name);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp 
ON audit_log(timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_org_id 
ON audit_log(org_id);
