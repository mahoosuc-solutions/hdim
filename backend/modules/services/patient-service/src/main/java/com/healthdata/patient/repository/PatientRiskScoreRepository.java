package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientRiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRiskScoreRepository extends JpaRepository<PatientRiskScoreEntity, UUID> {

    Optional<PatientRiskScoreEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientRiskScoreEntity> findByPatientIdAndTenantId(UUID patientId, String tenantId);
}
