package com.healthdata.quality.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Measure Execution History Repository
 * Complete audit trail of every measure calculation (HIPAA compliance).
 */
@Repository
public interface MeasureExecutionHistoryRepository extends JpaRepository<MeasureExecutionHistoryEntity, UUID> {

    // Tenant-isolated queries
    Optional<MeasureExecutionHistoryEntity> findByIdAndTenantId(UUID id, String tenantId);

    // Patient execution history
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        Pageable pageable
    );

    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "AND h.measureId = :measureId ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByPatientAndMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId,
        Pageable pageable
    );

    // Measure execution history
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.measureId = :measureId " +
           "ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByMeasure(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId,
        Pageable pageable
    );

    // By execution status
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.executionStatus = :status " +
           "ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByStatus(
        @Param("tenantId") String tenantId,
        @Param("status") String status,
        Pageable pageable
    );

    // Failed executions
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.executionStatus = 'FAILURE' " +
           "ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findFailedExecutions(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    // By execution mode
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.executionMode = :mode " +
           "ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByExecutionMode(
        @Param("tenantId") String tenantId,
        @Param("mode") String mode,
        Pageable pageable
    );

    // Time range queries
    @Query("SELECT h FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId " +
           "AND h.executionTimestamp BETWEEN :startTime AND :endTime ORDER BY h.executionTimestamp DESC")
    List<MeasureExecutionHistoryEntity> findByTimeRange(
        @Param("tenantId") String tenantId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    // Performance analytics
    @Query("SELECT AVG(h.durationMs) FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId " +
           "AND h.measureId = :measureId AND h.executionStatus = 'SUCCESS'")
    Double getAverageDuration(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    // Count executions
    @Query("SELECT COUNT(h) FROM MeasureExecutionHistoryEntity h WHERE h.tenantId = :tenantId AND h.patientId = :patientId")
    long countByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );
}
