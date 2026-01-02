package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.SdohDiagnosisEntity;
import com.healthdata.sdoh.model.SdohDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SdohDiagnosisRepository extends JpaRepository<SdohDiagnosisEntity, String> {

    List<SdohDiagnosisEntity> findByTenantIdAndPatientId(String tenantId, String patientId);

    List<SdohDiagnosisEntity> findByTenantIdAndPatientIdAndStatus(
            String tenantId, String patientId, SdohDiagnosis.DiagnosisStatus status);
}
