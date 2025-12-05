package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Health Score History entities
 */
@Repository
public interface HealthScoreHistoryRepository extends JpaRepository<HealthScoreHistoryEntity, UUID> {

    /**
     * Find health score history for a patient (tenant-isolated)
     */
    @Query("SELECT h FROM HealthScoreHistoryEntity h " +
           "WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "ORDER BY h.calculatedAt DESC")
    List<HealthScoreHistoryEntity> findByPatientIdOrderByCalculatedAtDesc(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Find history in date range
     */
    @Query("SELECT h FROM HealthScoreHistoryEntity h " +
           "WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "AND h.calculatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.calculatedAt DESC")
    List<HealthScoreHistoryEntity> findByPatientIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    /**
     * Get trend data (recent N scores)
     */
    @Query("SELECT h FROM HealthScoreHistoryEntity h " +
           "WHERE h.tenantId = :tenantId AND h.patientId = :patientId " +
           "ORDER BY h.calculatedAt DESC LIMIT :limit")
    List<HealthScoreHistoryEntity> findRecentScores(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("limit") int limit
    );
}
