package com.hackathon.gateway.service;

import com.hackathon.gateway.entity.Patient;
import com.hackathon.gateway.entity.PatientConsent;
import com.hackathon.gateway.repository.PatientConsentRepository;
import com.hackathon.gateway.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The heart of the gateway: for every protected field on a patient record,
 * asks OPA for a decision (which itself factors in both org-level policy
 * AND patient consent), then applies plain / encrypted / hidden accordingly,
 * and records everything to the audit log.
 *
 * <p>Enterprise enhancements:
 * <ul>
 *   <li>Consent lookups cached in Redis ({@code patient_consent} cache, 5-min TTL)</li>
 *   <li>Structured SLF4J logging for observability pipelines</li>
 * </ul>
 */
@Service
public class GatewayService {

    private static final Logger log = LoggerFactory.getLogger(GatewayService.class);

    private static final List<String> PROTECTED_FIELDS =
            List.of("name", "age", "disease", "aadhaar", "phone", "address");

    private final PatientRepository patientRepository;
    private final PatientConsentRepository consentRepository;
    private final OpaService opaService;
    private final FpeService fpeService;
    private final AuditService auditService;

    public GatewayService(PatientRepository patientRepository,
                           PatientConsentRepository consentRepository,
                           OpaService opaService,
                           FpeService fpeService,
                           AuditService auditService) {
        this.patientRepository = patientRepository;
        this.consentRepository = consentRepository;
        this.opaService = opaService;
        this.fpeService = fpeService;
        this.auditService = auditService;
    }

    public Map<String, Object> fetchProtectedRecord(Integer patientId, String orgId,
                                                      String userId, String ipAddress) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NoSuchElementException("Patient not found: " + patientId));

        log.info("[Gateway] Request: patientId={}, orgId={}, userId={}", patientId, orgId, userId);

        Map<String, Object> rawValues = new LinkedHashMap<>();
        rawValues.put("name", patient.getName());
        rawValues.put("age", patient.getAge() == null ? null : String.valueOf(patient.getAge()));
        rawValues.put("disease", patient.getDisease());
        rawValues.put("aadhaar", patient.getAadhaar());
        rawValues.put("phone", patient.getPhone());
        rawValues.put("address", patient.getAddress());

        Map<String, Object> result = new LinkedHashMap<>();
        StringBuilder masked = new StringBuilder();
        StringBuilder removed = new StringBuilder();

        for (String field : PROTECTED_FIELDS) {
            boolean consentGiven = lookupConsent(Integer.valueOf(patientId), field);
            String mode = opaService.getFieldMode(orgId, field, consentGiven);
            Object rawValue = rawValues.get(field);

            switch (mode) {
                case "plain" -> result.put(field, rawValue);
                case "encrypted" -> {
                    String encrypted = rawValue == null ? null : fpeService.encryptDigits(rawValue.toString());
                    result.put(field, encrypted);
                    appendCsv(masked, field);
                }
                default -> {
                    result.put(field, "— removed —");
                    appendCsv(removed, field);
                }
            }
        }

        log.info("[Gateway] Response: patientId={}, orgId={}, masked=[{}], removed=[{}]",
                patientId, orgId, masked, removed);

        auditService.record(
                orgId,
                userId == null ? "anonymous" : userId,
                "GET /api/gateway/patient/" + patientId,
                "gateway.rego",
                masked.toString(),
                removed.toString(),
                "200 OK",
                ipAddress
        );

        return result;
    }

    @Cacheable(value = "patient_consent", key = "#patientId + '-' + #field")
    public boolean lookupConsent(Integer patientId, String field) {
        return consentRepository.findByPatientIdAndFieldName(patientId, field)
                .map(PatientConsent::getConsentGiven)
                .orElse(false); // no consent row on file => default deny, safest default
    }

    private void appendCsv(StringBuilder sb, String value) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(value);
    }
}
