-- Run this against gateway_db after the four tables already exist
-- (patients, org_policies, patient_consent, audit_log)

INSERT INTO patients (name, age, disease, aadhaar, phone, address) VALUES
('Rahul', 35, 'Diabetes', '123456789012', '9876543210', 'Chennai'),
('Priya', 28, 'Asthma', '234567890123', '9123456780', 'Bengaluru'),
('Arjun', 42, 'Hypertension', '345678901234', '9988776655', 'Hyderabad');

-- Patient 1 (Rahul) consents to sharing name, age, disease, and aadhaar
-- with the insurer, but has NOT consented to sharing disease with anyone else.
INSERT INTO patient_consent (patient_id, field_name, consent_given) VALUES
(1, 'name', true),
(1, 'age', true),
(1, 'disease', true),
(1, 'aadhaar', true),
(1, 'phone', false),
(1, 'address', false);

-- Patient 2 (Priya) has turned OFF consent for disease specifically -
-- this is the exact case that proves the consent-aware AND-gate:
-- the insurer's org policy allows "disease", but Priya said no.
INSERT INTO patient_consent (patient_id, field_name, consent_given) VALUES
(2, 'name', true),
(2, 'age', true),
(2, 'disease', false),
(2, 'aadhaar', true),
(2, 'phone', false),
(2, 'address', false);
