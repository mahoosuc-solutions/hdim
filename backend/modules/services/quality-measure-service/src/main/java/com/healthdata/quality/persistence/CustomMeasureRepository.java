package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomMeasureRepository extends JpaRepository<CustomMeasureEntity, UUID> {
    List<CustomMeasureEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<CustomMeasureEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
    Optional<CustomMeasureEntity> findByTenantIdAndId(String tenantId, UUID id);

    // Batch operations support
    List<CustomMeasureEntity> findByTenantIdAndIdIn(String tenantId, List<UUID> ids);

    /**
     * Count evaluations that reference custom measures.
     * Custom measures use UUID ids, but quality_measure_results stores measureId as String.
     * We convert UUIDs to strings for the comparison.
     * Returns 0 if no evaluations reference these custom measures.
     */
    @Query("SELECT COUNT(r) FROM QualityMeasureResultEntity r WHERE r.measureId IN " +
           "(SELECT CAST(m.id AS string) FROM CustomMeasureEntity m WHERE m.id IN :measureIds)")
    long countEvaluationsByMeasureIds(@Param("measureIds") List<UUID> measureIds);
}
