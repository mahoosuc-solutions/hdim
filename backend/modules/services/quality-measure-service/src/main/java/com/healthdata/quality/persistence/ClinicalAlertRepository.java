package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Clinical Alerts
 *
 * Provides queries for alert retrieval, deduplication, and filtering.
 */
@Repository
public interface ClinicalAlertRepository extends JpaRepository<ClinicalAlertEntity, UUID> {

    /**
     * Find all active alerts for a patient (ordered by severity and time)
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.tenantId = :tenantId
        AND a.patientId = :patientId
        AND a.status = 'ACTIVE'
        ORDER BY
            CASE a.severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
            END,
            a.triggeredAt DESC
    """)
    List<ClinicalAlertEntity> findActiveAlertsForPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find recent duplicate alerts (for deduplication within 24-hour window)
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.tenantId = :tenantId
        AND a.patientId = :patientId
        AND a.alertType = :alertType
        AND a.triggeredAt >= :since
        AND a.status IN ('ACTIVE', 'ACKNOWLEDGED')
        ORDER BY a.triggeredAt DESC
    """)
    List<ClinicalAlertEntity> findRecentDuplicates(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("alertType") ClinicalAlertEntity.AlertType alertType,
        @Param("since") Instant since
    );

    /**
     * Find all alerts for a patient (any status)
     */
    List<ClinicalAlertEntity> findByTenantIdAndPatientIdOrderByTriggeredAtDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Find alerts by type
     */
    List<ClinicalAlertEntity> findByTenantIdAndAlertTypeOrderByTriggeredAtDesc(
        String tenantId,
        ClinicalAlertEntity.AlertType alertType
    );

    /**
     * Find alerts by severity
     */
    List<ClinicalAlertEntity> findByTenantIdAndSeverityAndStatusOrderByTriggeredAtDesc(
        String tenantId,
        ClinicalAlertEntity.AlertSeverity severity,
        ClinicalAlertEntity.AlertStatus status
    );

    /**
     * Find critical unacknowledged alerts across all patients
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.tenantId = :tenantId
        AND a.severity = 'CRITICAL'
        AND a.status = 'ACTIVE'
        ORDER BY a.triggeredAt DESC
    """)
    List<ClinicalAlertEntity> findCriticalUnacknowledgedAlerts(
        @Param("tenantId") String tenantId
    );

    /**
     * Count active alerts for a patient
     */
    long countByTenantIdAndPatientIdAndStatus(
        String tenantId,
        UUID patientId,
        ClinicalAlertEntity.AlertStatus status
    );

    /**
     * Count alerts by severity
     */
    long countByTenantIdAndSeverityAndStatus(
        String tenantId,
        ClinicalAlertEntity.AlertSeverity severity,
        ClinicalAlertEntity.AlertStatus status
    );

    /**
     * Find escalated alerts
     */
    List<ClinicalAlertEntity> findByTenantIdAndEscalatedAndStatusOrderByEscalatedAtDesc(
        String tenantId,
        boolean escalated,
        ClinicalAlertEntity.AlertStatus status
    );

    /**
     * Find alerts triggered in date range
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.tenantId = :tenantId
        AND a.triggeredAt BETWEEN :startDate AND :endDate
        ORDER BY a.triggeredAt DESC
    """)
    List<ClinicalAlertEntity> findByDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Find all unacknowledged alerts older than threshold
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.tenantId = :tenantId
        AND a.status = 'ACTIVE'
        AND a.triggeredAt < :threshold
        ORDER BY a.severity, a.triggeredAt
    """)
    List<ClinicalAlertEntity> findUnacknowledgedOlderThan(
        @Param("tenantId") String tenantId,
        @Param("threshold") Instant threshold
    );

    /**
     * Find all unacknowledged, non-escalated alerts (for escalation processing)
     */
    @Query("""
        SELECT a FROM ClinicalAlertEntity a
        WHERE a.status = 'ACTIVE'
        AND a.escalated = false
        ORDER BY a.severity, a.triggeredAt
    """)
    List<ClinicalAlertEntity> findUnacknowledgedAlerts(
        @Param("now") Instant now
    );
}
