package com.hackathon.gateway.repository;

import com.hackathon.gateway.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
}
