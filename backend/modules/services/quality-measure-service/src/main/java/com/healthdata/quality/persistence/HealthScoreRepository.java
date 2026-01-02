package com.healthdata.quality.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Health Score entities
 */
@Repository
public interface HealthScoreRepository extends JpaRepository<HealthScoreEntity, UUID> {

    /**
     * Find the latest health score for a patient (tenant-isolated)
     */
    @Query("SELECT h FROM HealthScoreEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "ORDER BY h.calculatedAt DESC LIMIT 1")
    Optional<HealthScoreEntity> findLatestByPatientId(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find all health scores for a patient (tenant-isolated)
     */
    @Query("SELECT h FROM HealthScoreEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "ORDER BY h.calculatedAt DESC")
    List<HealthScoreEntity> findByPatientIdOrderByCalculatedAtDesc(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find health scores with significant changes
     */
    @Query("SELECT h FROM HealthScoreEntity h WHERE h.tenantId = :tenantId " +
           "AND h.significantChange = true AND h.calculatedAt >= :since " +
           "ORDER BY h.calculatedAt DESC")
    List<HealthScoreEntity> findSignificantChangesSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Find health scores in a date range
     */
    @Query("SELECT h FROM HealthScoreEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "AND h.calculatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.calculatedAt DESC")
    List<HealthScoreEntity> findByPatientIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Count patients with scores below threshold
     */
    @Query("SELECT COUNT(DISTINCT h.patientId) FROM HealthScoreEntity h " +
           "WHERE h.tenantId = :tenantId AND h.overallScore < :threshold " +
           "AND h.id IN (SELECT h2.id FROM HealthScoreEntity h2 " +
           "WHERE h2.tenantId = :tenantId AND h2.patientId = h.patientId " +
           "ORDER BY h2.calculatedAt DESC LIMIT 1)")
    long countPatientsWithScoreBelowThreshold(
        @Param("tenantId") String tenantId,
        @Param("threshold") Double threshold
    );

    /**
     * Find latest health scores for patients with scores below threshold (paginated)
     * Returns only the most recent score for each patient
     */
    @Query(value = "SELECT h.* FROM health_scores h " +
           "INNER JOIN (SELECT patient_id, MAX(calculated_at) as max_calc " +
           "FROM health_scores WHERE tenant_id = :tenantId " +
           "GROUP BY patient_id) latest " +
           "ON h.patient_id = latest.patient_id AND h.calculated_at = latest.max_calc " +
           "WHERE h.tenant_id = :tenantId AND h.overall_score < :threshold",
           nativeQuery = true)
    Page<HealthScoreEntity> findLatestScoresBelowThreshold(
        @Param("tenantId") String tenantId,
        @Param("threshold") Double threshold,
        Pageable pageable
    );

    /**
     * Find health scores with significant changes since a given time (paginated)
     */
    @Query("SELECT h FROM HealthScoreEntity h WHERE h.tenantId = :tenantId " +
           "AND h.significantChange = true AND h.calculatedAt >= :since")
    Page<HealthScoreEntity> findSignificantChangesSince(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since,
        Pageable pageable
    );
}
