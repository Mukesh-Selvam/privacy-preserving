package com.hackathon.gateway.repository;

import com.hackathon.gateway.entity.PatientConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, Integer> {

    Optional<PatientConsent> findByPatientIdAndFieldName(Integer patientId, String fieldName);

    List<PatientConsent> findByPatientId(Integer patientId);
}
