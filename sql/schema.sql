-- Auto-run by docker-compose on first Postgres container start
-- (mounted into /docker-entrypoint-initdb.d/)

CREATE TABLE IF NOT EXISTS patients (
    patient_id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    age INT,
    disease VARCHAR(200),
    aadhaar VARCHAR(20),
    phone VARCHAR(20),
    address VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS org_policies (
    id SERIAL PRIMARY KEY,
    org_id VARCHAR(100) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    allowed BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS patient_consent (
    id SERIAL PRIMARY KEY,
    patient_id INT REFERENCES patients(patient_id),
    field_name VARCHAR(100) NOT NULL,
    consent_given BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT now(),
    org_id VARCHAR(100),
    user_id VARCHAR(100),
    api_called VARCHAR(200),
    policy_applied VARCHAR(200),
    fields_masked VARCHAR(500),
    fields_removed VARCHAR(500),
    status VARCHAR(50),
    ip_address VARCHAR(50)
);

-- seed data, loaded automatically on first container start
INSERT INTO patients (name, age, disease, aadhaar, phone, address) VALUES
('Rahul', 35, 'Diabetes', '123456789012', '9876543210', 'Chennai'),
('Priya', 28, 'Asthma', '234567890123', '9123456780', 'Bengaluru'),
('Arjun', 42, 'Hypertension', '345678901234', '9988776655', 'Hyderabad');

INSERT INTO patient_consent (patient_id, field_name, consent_given) VALUES
(1, 'name', true),
(1, 'age', true),
(1, 'disease', true),
(1, 'aadhaar', true),
(1, 'phone', false),
(1, 'address', false),
(2, 'name', true),
(2, 'age', true),
(2, 'disease', false),
(2, 'aadhaar', true),
(2, 'phone', false),
(2, 'address', false),
(3, 'name', true),
(3, 'age', true),
(3, 'disease', true),
(3, 'aadhaar', false),
(3, 'phone', false),
(3, 'address', false);
