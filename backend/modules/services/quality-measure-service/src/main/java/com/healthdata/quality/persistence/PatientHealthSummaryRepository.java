package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Patient Health Summary Repository (CQRS Read Model)
 *
 * Optimized queries for patient health summaries.
 * All queries are simple and fast (no joins needed).
 */
@Repository
public interface PatientHealthSummaryRepository extends JpaRepository<PatientHealthSummaryEntity, Long> {

    /**
     * Find summary by tenant and patient ID
     */
    Optional<PatientHealthSummaryEntity> findByTenantIdAndPatientId(String tenantId, String patientId);

    /**
     * Find all summaries for a tenant
     */
    List<PatientHealthSummaryEntity> findByTenantIdOrderByLastUpdatedAtDesc(String tenantId);

    /**
     * Find high-risk patients
     */
    List<PatientHealthSummaryEntity> findByTenantIdAndRiskLevelOrderByRiskScoreDesc(
        String tenantId,
        String riskLevel
    );

    /**
     * Find patients with urgent care gaps
     */
    @Query("SELECT phs FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.urgentGapsCount > 0 " +
           "ORDER BY phs.urgentGapsCount DESC")
    List<PatientHealthSummaryEntity> findPatientsWithUrgentGaps(@Param("tenantId") String tenantId);

    /**
     * Find patients with critical alerts
     */
    @Query("SELECT phs FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.criticalAlertsCount > 0 " +
           "ORDER BY phs.criticalAlertsCount DESC")
    List<PatientHealthSummaryEntity> findPatientsWithCriticalAlerts(@Param("tenantId") String tenantId);

    /**
     * Count total patients for tenant
     */
    Long countByTenantId(String tenantId);

    /**
     * Calculate average health score for tenant
     */
    @Query("SELECT AVG(phs.latestHealthScore) FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.latestHealthScore IS NOT NULL")
    Double averageHealthScoreByTenantId(@Param("tenantId") String tenantId);

    /**
     * Count high-risk patients
     */
    @Query("SELECT COUNT(phs) FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.riskLevel = 'high'")
    Long countHighRiskPatients(@Param("tenantId") String tenantId);

    /**
     * Count medium-risk patients
     */
    @Query("SELECT COUNT(phs) FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.riskLevel = 'medium'")
    Long countMediumRiskPatients(@Param("tenantId") String tenantId);

    /**
     * Sum total open care gaps
     */
    @Query("SELECT SUM(phs.openCareGapsCount) FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId")
    Long totalOpenCareGaps(@Param("tenantId") String tenantId);

    /**
     * Find patients by health score range
     */
    @Query("SELECT phs FROM PatientHealthSummaryEntity phs " +
           "WHERE phs.tenantId = :tenantId " +
           "AND phs.latestHealthScore BETWEEN :minScore AND :maxScore " +
           "ORDER BY phs.latestHealthScore ASC")
    List<PatientHealthSummaryEntity> findByHealthScoreRange(
        @Param("tenantId") String tenantId,
        @Param("minScore") Double minScore,
        @Param("maxScore") Double maxScore
    );

    /**
     * Find patients with declining health trend
     */
    List<PatientHealthSummaryEntity> findByTenantIdAndHealthTrendOrderByLatestHealthScoreAsc(
        String tenantId,
        String healthTrend
    );
}
