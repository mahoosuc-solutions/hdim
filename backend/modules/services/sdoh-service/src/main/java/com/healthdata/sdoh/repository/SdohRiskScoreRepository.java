package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.SdohRiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SdohRiskScoreRepository extends JpaRepository<SdohRiskScoreEntity, String> {

    List<SdohRiskScoreEntity> findByTenantIdAndPatientIdOrderByCalculatedAtDesc(
            String tenantId, String patientId);
}
