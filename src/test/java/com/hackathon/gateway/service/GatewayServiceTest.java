package com.hackathon.gateway.service;

import com.hackathon.gateway.entity.Patient;
import com.hackathon.gateway.entity.PatientConsent;
import com.hackathon.gateway.repository.PatientConsentRepository;
import com.hackathon.gateway.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GatewayService.
 *
 * <p>Verifies the core dual-gating logic: OPA mode decisions, consent lookups,
 * field transformations, and audit trail recording.
 */
@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock PatientRepository patientRepository;
    @Mock PatientConsentRepository consentRepository;
    @Mock OpaService opaService;
    @Mock FpeService fpeService;
    @Mock AuditService auditService;

    @InjectMocks
    private GatewayService gatewayService;

    private Patient rahul;

    @BeforeEach
    void setUp() {
        rahul = new Patient();
        rahul.setName("Rahul");
        rahul.setAge(35);
        rahul.setDisease("Diabetes");
        rahul.setAadhaar("123456789012");
        rahul.setPhone("9876543210");
        rahul.setAddress("Chennai");
    }

    @Test
    @DisplayName("'plain' mode returns raw field value unchanged")
    void plainMode_returnsRawValue() {
        when(patientRepository.findById(1)).thenReturn(Optional.of(rahul));
        // Default all fields to hidden with no consent
        lenient().when(consentRepository.findByPatientIdAndFieldName(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(opaService.getFieldMode(anyString(), anyString(), anyBoolean()))
                .thenReturn("hidden");
        // Override specifically for 'name' field
        when(consentRepository.findByPatientIdAndFieldName(1, "name"))
                .thenReturn(Optional.of(consentRow(1, "name", true)));
        when(opaService.getFieldMode("insurer-partner", "name", true)).thenReturn("plain");

        Map<String, Object> result = gatewayService.fetchProtectedRecord(1, "insurer-partner", "user", "127.0.0.1");
        assertThat(result.get("name")).isEqualTo("Rahul");
    }

    @Test
    @DisplayName("'hidden' mode returns redaction placeholder")
    void hiddenMode_returnsRedacted() {
        when(patientRepository.findById(1)).thenReturn(Optional.of(rahul));
        when(consentRepository.findByPatientIdAndFieldName(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(opaService.getFieldMode(anyString(), anyString(), eq(false)))
                .thenReturn("hidden");

        Map<String, Object> result = gatewayService.fetchProtectedRecord(1, "research-org", null, "10.0.0.1");
        assertThat(result.get("phone")).isEqualTo("— removed —");
        assertThat(result.get("address")).isEqualTo("— removed —");
    }

    @Test
    @DisplayName("'encrypted' mode delegates to FpeService and returns encrypted value")
    void encryptedMode_callsFpeService() {
        when(patientRepository.findById(1)).thenReturn(Optional.of(rahul));
        // Default all to hidden/no-consent
        lenient().when(consentRepository.findByPatientIdAndFieldName(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(opaService.getFieldMode(anyString(), anyString(), anyBoolean()))
                .thenReturn("hidden");
        // Override specifically for aadhaar
        when(consentRepository.findByPatientIdAndFieldName(1, "aadhaar"))
                .thenReturn(Optional.of(consentRow(1, "aadhaar", true)));
        when(opaService.getFieldMode("insurer-partner", "aadhaar", true)).thenReturn("encrypted");
        when(fpeService.encryptDigits("123456789012")).thenReturn("781495260378");

        Map<String, Object> result = gatewayService.fetchProtectedRecord(1, "insurer-partner", "agent", "127.0.0.1");
        assertThat(result.get("aadhaar")).isEqualTo("781495260378");
        verify(fpeService).encryptDigits("123456789012");
    }

    @Test
    @DisplayName("No consent row defaults to false (deny-by-default)")
    void noConsentRow_defaultsDeny() {
        when(patientRepository.findById(1)).thenReturn(Optional.of(rahul));
        when(consentRepository.findByPatientIdAndFieldName(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(opaService.getFieldMode(anyString(), anyString(), eq(false))).thenReturn("hidden");

        gatewayService.fetchProtectedRecord(1, "insurer-partner", "user", "127.0.0.1");
        // OPA should be called with consentGiven=false for all fields
        verify(opaService, atLeast(1)).getFieldMode(eq("insurer-partner"), anyString(), eq(false));
    }

    @Test
    @DisplayName("Non-existent patient throws NoSuchElementException")
    void missingPatient_throwsException() {
        when(patientRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
                gatewayService.fetchProtectedRecord(999, "insurer-partner", "user", "127.0.0.1"))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Audit is always recorded regardless of field decisions")
    void audit_alwaysRecorded() {
        when(patientRepository.findById(1)).thenReturn(Optional.of(rahul));
        when(consentRepository.findByPatientIdAndFieldName(anyInt(), anyString()))
                .thenReturn(Optional.empty());
        when(opaService.getFieldMode(anyString(), anyString(), anyBoolean())).thenReturn("hidden");

        gatewayService.fetchProtectedRecord(1, "insurer-partner", "user", "127.0.0.1");
        verify(auditService, times(1)).record(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString());
    }

    private PatientConsent consentRow(int patientId, String field, boolean given) {
        PatientConsent c = new PatientConsent();
        c.setPatientId(patientId);
        c.setFieldName(field);
        c.setConsentGiven(given);
        return c;
    }
}
