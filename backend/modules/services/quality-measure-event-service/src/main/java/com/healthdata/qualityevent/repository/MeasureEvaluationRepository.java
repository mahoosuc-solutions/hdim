package com.healthdata.qualityevent.repository;

import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Measure Evaluation Projection (CQRS Read Model)
 *
 * Optimized for fast queries on measure evaluation data.
 * All queries include tenant isolation for multi-tenancy.
 */
@Repository
public interface MeasureEvaluationRepository extends JpaRepository<MeasureEvaluationProjection, Long> {

    /**
     * Find measure evaluation by tenant, measure, and patient
     */
    Optional<MeasureEvaluationProjection> findByTenantIdAndMeasureIdAndPatientId(
        String tenantId, String measureId, UUID patientId);

    /**
     * Find all evaluations for a patient
     */
    List<MeasureEvaluationProjection> findByTenantIdAndPatientIdOrderByMeasureIdAsc(
        String tenantId, UUID patientId);

    /**
     * Find all evaluations for a measure (paginated)
     */
    Page<MeasureEvaluationProjection> findByTenantIdAndMeasureIdOrderByPatientIdAsc(
        String tenantId, String measureId, Pageable pageable);

    /**
     * Find non-compliant patients for a measure
     */
    @Query("SELECT m FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId AND m.isCompliant = false ORDER BY m.patientId ASC")
    List<MeasureEvaluationProjection> findNonCompliantForMeasure(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Find compliant patients for a measure
     */
    @Query("SELECT m FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId AND m.isCompliant = true ORDER BY m.patientId ASC")
    List<MeasureEvaluationProjection> findCompliantForMeasure(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Count compliant patients for a measure
     */
    @Query("SELECT COUNT(m) FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId AND m.isCompliant = true")
    long countCompliantForMeasure(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Count total patients evaluated for a measure
     */
    @Query("SELECT COUNT(m) FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId")
    long countTotalForMeasure(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Calculate compliance rate for a measure
     */
    @Query("SELECT AVG(CASE WHEN m.isCompliant = true THEN 1.0 ELSE 0.0 END) FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId")
    Double calculateComplianceRate(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Get average score for a measure
     */
    @Query("SELECT AVG(m.score) FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.measureId = :measureId")
    Double getAverageScore(@Param("tenantId") String tenantId, @Param("measureId") String measureId);

    /**
     * Find all evaluations by compliance status
     */
    @Query("SELECT m FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.complianceStatus = :status ORDER BY m.measureId ASC, m.patientId ASC")
    List<MeasureEvaluationProjection> findByComplianceStatus(@Param("tenantId") String tenantId, @Param("status") String status);

    /**
     * Get measures trending (average score trend)
     */
    @Query("SELECT DISTINCT m.measureId FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId ORDER BY m.measureId")
    List<String> findDistinctMeasures(@Param("tenantId") String tenantId);

    /**
     * Find measures not meeting threshold
     */
    @Query("SELECT DISTINCT m.measureId FROM MeasureEvaluationProjection m WHERE m.tenantId = :tenantId AND m.meetsThreshold = false")
    List<String> findMeasuresNotMeetingThreshold(@Param("tenantId") String tenantId);

    /**
     * Get distinct tenant IDs (for rebuild operations)
     */
    @Query("SELECT DISTINCT m.tenantId FROM MeasureEvaluationProjection m")
    List<String> findDistinctTenantIds();

    /**
     * Delete all projections for a tenant
     */
    void deleteAllByTenantId(String tenantId);

    /**
     * Delete all projections for a patient
     */
    void deleteAllByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Delete all projections for a measure
     */
    void deleteAllByTenantIdAndMeasureId(String tenantId, String measureId);
}
