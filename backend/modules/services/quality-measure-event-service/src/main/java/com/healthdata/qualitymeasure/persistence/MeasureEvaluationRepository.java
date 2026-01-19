package com.healthdata.qualitymeasure.persistence;

import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Measure Evaluation Repository
 *
 * Persistence layer for MeasureEvaluationProjection (read model)
 * Enables fast queries of measure evaluation results
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface MeasureEvaluationRepository extends JpaRepository<MeasureEvaluationProjection, String> {

    /**
     * Find measure evaluation by patient ID and tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT m FROM MeasureEvaluationProjection m WHERE m.patientId = :patientId AND m.tenantId = :tenantId")
    Optional<MeasureEvaluationProjection> findByPatientIdAndTenant(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all evaluations for a measure code by tenant
     */
    @Query("SELECT m FROM MeasureEvaluationProjection m WHERE m.measureCode = :measureCode AND m.tenantId = :tenantId ORDER BY m.lastUpdated DESC")
    List<MeasureEvaluationProjection> findByMeasureCodeAndTenant(
        @Param("measureCode") String measureCode,
        @Param("tenantId") String tenantId
    );

    /**
     * Count met evaluations for a measure and tenant
     */
    @Query("SELECT COUNT(m) FROM MeasureEvaluationProjection m WHERE m.measureCode = :measureCode AND m.tenantId = :tenantId AND m.status = 'MET'")
    long countMetByMeasureCodeAndTenant(
        @Param("measureCode") String measureCode,
        @Param("tenantId") String tenantId
    );
}
