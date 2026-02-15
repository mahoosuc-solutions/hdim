package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientInsuranceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientInsuranceRepository extends JpaRepository<PatientInsuranceEntity, UUID> {

    Optional<PatientInsuranceEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<PatientInsuranceEntity> findByPatientIdAndTenantId(UUID patientId, String tenantId);
}
