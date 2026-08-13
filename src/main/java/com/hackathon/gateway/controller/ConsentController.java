package com.hackathon.gateway.controller;

import com.hackathon.gateway.dto.ApiResponse;
import com.hackathon.gateway.dto.ConsentRequest;
import com.hackathon.gateway.entity.PatientConsent;
import com.hackathon.gateway.repository.PatientConsentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Patient consent management endpoints.
 *
 * <p>Mutation operations immediately evict the corresponding Redis cache entry,
 * guaranteeing that the next gateway request reflects the updated consent
 * without any stale-cache window.
 */
@RestController
@RequestMapping("/api/consent")
@Tag(name = "Consent Management", description = "Patient field-level consent CRUD operations")
public class ConsentController {

    private final PatientConsentRepository consentRepository;

    public ConsentController(PatientConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    @Operation(
        summary = "Set patient consent for a field",
        description = """
            Creates or updates a patient's consent flag for a specific data field.
            The change is reflected **immediately** on the next gateway request —
            the corresponding cache entry is evicted atomically.
            """
    )
    @PostMapping
    @CacheEvict(value = "patient_consent", key = "#req.patientId() + '-' + #req.field()")
    public ResponseEntity<ApiResponse<PatientConsent>> setConsent(
            @Valid @RequestBody ConsentRequest req) {
        PatientConsent consent = consentRepository
                .findByPatientIdAndFieldName(req.patientId(), req.field())
                .orElse(new PatientConsent());

        consent.setPatientId(req.patientId());
        consent.setFieldName(req.field());
        consent.setConsentGiven(req.consentGiven());
        consent.setUpdatedAt(LocalDateTime.now());

        consentRepository.save(consent);
        return ResponseEntity.ok(ApiResponse.ok(consent, "Consent updated successfully"));
    }

    @Operation(
        summary = "Get all consent settings for a patient",
        description = "Returns the full list of field-level consent records for the given patient."
    )
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientConsent>>> getConsent(
            @Parameter(description = "Numeric patient identifier", example = "1")
            @PathVariable Integer patientId) {
        List<PatientConsent> consents = consentRepository.findByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.ok(consents));
    }

    @Operation(
        summary = "Get patient consent as a field→boolean map",
        description = "Returns a flat map of {fieldName: consentGiven} for the patient. " +
                      "Fields not present in the table are treated as consented (true) by default. " +
                      "Used by the Chrome extension patient portal."
    )
    @GetMapping("/{patientId}/map")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getConsentMap(
            @Parameter(description = "Numeric patient identifier", example = "1")
            @PathVariable Integer patientId) {
        List<PatientConsent> consents = consentRepository.findByPatientId(patientId);
        Map<String, Boolean> map = consents.stream()
                .collect(Collectors.toMap(
                        PatientConsent::getFieldName,
                        PatientConsent::getConsentGiven
                ));
        // Apply deny-by-default: if a field has no record, it defaults to true (patient hasn't opted-out)
        List.of("name", "age", "disease", "aadhaar", "phone", "address")
                .forEach(field -> map.putIfAbsent(field, true));
        return ResponseEntity.ok(ApiResponse.ok(map, "Consent map for patient " + patientId));
    }
}
