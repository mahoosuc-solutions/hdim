package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientRiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRiskScoreRepository extends JpaRepository<PatientRiskScoreEntity, UUID> {
}
