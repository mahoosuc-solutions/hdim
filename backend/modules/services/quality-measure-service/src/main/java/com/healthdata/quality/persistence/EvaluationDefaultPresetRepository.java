package com.healthdata.quality.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationDefaultPresetRepository extends JpaRepository<EvaluationDefaultPresetEntity, UUID> {
    Optional<EvaluationDefaultPresetEntity> findByTenantIdAndUserId(String tenantId, String userId);

    boolean existsByTenantIdAndUserId(String tenantId, String userId);

    void deleteByTenantIdAndUserId(String tenantId, String userId);
}
