package com.healthdata.events.intelligence.repository;

import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IntelligenceValidationFindingRepository extends JpaRepository<IntelligenceValidationFindingEntity, UUID> {

    List<IntelligenceValidationFindingEntity> findByTenantIdAndPatientRefOrderByCreatedAtDesc(String tenantId, String patientRef);

    List<IntelligenceValidationFindingEntity> findByTenantIdAndPatientRefAndStatusOrderByCreatedAtDesc(
            String tenantId,
            String patientRef,
            FindingStatus status
    );

    List<IntelligenceValidationFindingEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(
            String tenantId,
            FindingStatus status
    );

    java.util.Optional<IntelligenceValidationFindingEntity> findByIdAndTenantId(UUID id, String tenantId);
}
