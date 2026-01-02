package com.healthdata.quality.repository;

import com.healthdata.quality.domain.MeasureResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Quality Measure Result entities
 * Provides comprehensive access to calculated quality measure outcomes
 * Supports complex aggregations, pagination, and tenant isolation
 * Used for quality reporting, HEDIS measures, and compliance tracking
 */
@Repository
public interface QualityMeasureResultRepository extends JpaRepository<MeasureResult, String> {

    /**
     * Find measure results by patient ID and measure ID
     *
     * @param patientId Patient identifier
     * @param measureId Quality measure identifier (e.g., HEDIS-CDC)
     * @return List of measure results for the patient and measure
     */
    List<MeasureResult> findByPatientIdAndMeasureId(String patientId, String measureId);

    /**
     * Find measure results by measure ID for a specific tenant
     * Used for population-level quality reporting
     *
     * @param measureId Quality measure identifier
     * @param tenantId Tenant identifier
     * @param pageable Pagination information
     * @return Paginated measure results for the tenant
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.measureId = :measureId
        AND mr.tenantId = :tenantId
        ORDER BY mr.calculationDate DESC
        """)
    Page<MeasureResult> findByMeasureIdAndTenant(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find latest measure results for a patient
     * Returns the most recent results for all measures
     *
     * @param patientId Patient identifier
     * @param pageable Pagination information
     * @return Paginated list of latest results, ordered by calculation date
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        ORDER BY mr.calculationDate DESC
        """)
    Page<MeasureResult> findLatestResultsByPatient(
        @Param("patientId") String patientId,
        Pageable pageable
    );

    /**
     * Find latest measure result for a patient and measure ID
     *
     * @param patientId Patient identifier
     * @param measureId Quality measure identifier
     * @return Optional containing the latest result if found
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.measureId = :measureId
        ORDER BY mr.calculationDate DESC
        LIMIT 1
        """)
    Optional<MeasureResult> findLatestByPatientAndMeasure(
        @Param("patientId") String patientId,
        @Param("measureId") String measureId
    );

    /**
     * Find measure results within a date range
     *
     * @param patientId Patient identifier
     * @param startDate Start of calculation date range
     * @param endDate End of calculation date range
     * @return List of results calculated within the date range
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.calculationDate BETWEEN :startDate AND :endDate
        ORDER BY mr.calculationDate DESC
        """)
    List<MeasureResult> findByPatientIdAndCalculationDateRange(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find compliant results for a patient
     * Used for quality compliance tracking
     *
     * @param patientId Patient identifier
     * @return List of compliant measure results
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.compliant = true
        ORDER BY mr.calculationDate DESC
        """)
    List<MeasureResult> findCompliantResultsByPatient(@Param("patientId") String patientId);

    /**
     * Find non-compliant results for a patient
     * Used for identifying gaps and care opportunities
     *
     * @param patientId Patient identifier
     * @return List of non-compliant measure results
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.compliant = false
        ORDER BY mr.calculationDate DESC
        """)
    List<MeasureResult> findNonCompliantResultsByPatient(@Param("patientId") String patientId);

    /**
     * Find measure results by compliance status for a tenant
     *
     * @param tenantId Tenant identifier
     * @param compliant Compliance status
     * @param pageable Pagination information
     * @return Paginated results matching the compliance status
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.tenantId = :tenantId
        AND mr.compliant = :compliant
        ORDER BY mr.calculationDate DESC
        """)
    Page<MeasureResult> findByTenantIdAndCompliant(
        @Param("tenantId") String tenantId,
        @Param("compliant") boolean compliant,
        Pageable pageable
    );

    /**
     * Aggregate measure results by measure ID and tenant
     * Returns summary statistics for population-level reporting
     *
     * @param measureId Quality measure identifier
     * @param tenantId Tenant identifier
     * @return Object array containing count and compliance metrics
     */
    @Query("""
        SELECT COUNT(mr), SUM(CASE WHEN mr.compliant = true THEN 1 ELSE 0 END),
               AVG(mr.score), MIN(mr.score), MAX(mr.score)
        FROM MeasureResult mr
        WHERE mr.measureId = :measureId
        AND mr.tenantId = :tenantId
        """)
    Object[] aggregateMeasureResults(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId
    );

    /**
     * Count compliant measure results by measure and tenant
     *
     * @param measureId Quality measure identifier
     * @param tenantId Tenant identifier
     * @param compliant Compliance status
     * @return Count of compliant results
     */
    @Query("""
        SELECT COUNT(mr) FROM MeasureResult mr
        WHERE mr.measureId = :measureId
        AND mr.tenantId = :tenantId
        AND mr.compliant = :compliant
        """)
    long countByMeasureAndTenantAndCompliant(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId,
        @Param("compliant") boolean compliant
    );

    /**
     * Find measure results for a measurement period
     *
     * @param tenantId Tenant identifier
     * @param periodStart Start of measurement period
     * @param periodEnd End of measurement period
     * @return List of results for the measurement period
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.tenantId = :tenantId
        AND mr.periodStart >= :periodStart
        AND mr.periodEnd <= :periodEnd
        ORDER BY mr.calculationDate DESC
        """)
    List<MeasureResult> findByTenantAndMeasurementPeriod(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDate periodStart,
        @Param("periodEnd") LocalDate periodEnd
    );

    /**
     * Find measure results with low scores
     * Used for identifying performance gaps
     *
     * @param tenantId Tenant identifier
     * @param minScore Minimum score threshold
     * @return List of results below the threshold
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.tenantId = :tenantId
        AND mr.score < :minScore
        ORDER BY mr.score ASC
        """)
    List<MeasureResult> findBelowPerformanceThreshold(
        @Param("tenantId") String tenantId,
        @Param("minScore") Double minScore
    );

    /**
     * Calculate average score by measure for a tenant
     *
     * @param measureId Quality measure identifier
     * @param tenantId Tenant identifier
     * @return Average score or null if no results
     */
    @Query("""
        SELECT AVG(mr.score) FROM MeasureResult mr
        WHERE mr.measureId = :measureId
        AND mr.tenantId = :tenantId
        """)
    Double getAverageScoreByMeasure(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId
    );

    /**
     * Calculate compliance rate for a measure and tenant
     *
     * @param measureId Quality measure identifier
     * @param tenantId Tenant identifier
     * @return Compliance percentage (0-100)
     */
    @Query("""
        SELECT (COUNT(CASE WHEN mr.compliant = true THEN 1 END) * 100.0 / COUNT(mr))
        FROM MeasureResult mr
        WHERE mr.measureId = :measureId
        AND mr.tenantId = :tenantId
        """)
    Double getComplianceRate(
        @Param("measureId") String measureId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find measure results by denominator status
     * Used for identifying patients in measure populations
     *
     * @param patientId Patient identifier
     * @param measureId Quality measure identifier
     * @return List of results where patient is in denominator
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.measureId = :measureId
        AND mr.denominator IS NOT NULL
        AND mr.denominator > 0
        """)
    List<MeasureResult> findByPatientAndMeasureInDenominator(
        @Param("patientId") String patientId,
        @Param("measureId") String measureId
    );

    /**
     * Update measure result calculation date
     * Used for refreshing calculations
     *
     * @param resultId Measure result identifier
     * @param newCalculationDate New calculation date
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE MeasureResult mr
        SET mr.calculationDate = :newCalculationDate
        WHERE mr.id = :resultId
        """)
    void updateCalculationDate(
        @Param("resultId") String resultId,
        @Param("newCalculationDate") LocalDateTime newCalculationDate
    );

    /**
     * Bulk update compliance status
     * Used for remeasurement and status transitions
     *
     * @param tenantId Tenant identifier
     * @param measureId Quality measure identifier
     * @param compliant New compliance status
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE MeasureResult mr
        SET mr.compliant = :compliant, mr.calculationDate = CURRENT_TIMESTAMP
        WHERE mr.tenantId = :tenantId
        AND mr.measureId = :measureId
        """)
    void updateComplianceStatus(
        @Param("tenantId") String tenantId,
        @Param("measureId") String measureId,
        @Param("compliant") boolean compliant
    );

    /**
     * Count measure results by patient and measure
     *
     * @param patientId Patient identifier
     * @param measureId Quality measure identifier
     * @return Count of results
     */
    @Query("""
        SELECT COUNT(mr) FROM MeasureResult mr
        WHERE mr.patientId = :patientId
        AND mr.measureId = :measureId
        """)
    long countByPatientAndMeasure(
        @Param("patientId") String patientId,
        @Param("measureId") String measureId
    );

    /**
     * Find results ordered by score (descending) for ranking
     *
     * @param tenantId Tenant identifier
     * @param pageable Pagination information
     * @return Paginated results ranked by score
     */
    @Query("""
        SELECT mr FROM MeasureResult mr
        WHERE mr.tenantId = :tenantId
        ORDER BY mr.score DESC
        """)
    Page<MeasureResult> findTopPerformers(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );
}
