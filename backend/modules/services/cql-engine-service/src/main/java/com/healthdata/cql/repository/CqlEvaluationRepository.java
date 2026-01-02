package com.healthdata.cql.repository;

import com.healthdata.cql.entity.CqlEvaluation;
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
 * Repository for CQL Evaluation entities
 *
 * Provides multi-tenant access to CQL evaluation results with query methods
 * for patient-specific and library-specific lookups.
 */
@Repository
public interface CqlEvaluationRepository extends JpaRepository<CqlEvaluation, UUID> {

    /**
     * Find all evaluations for a tenant with pagination
     */
    Page<CqlEvaluation> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find all evaluations for a specific patient
     */
    List<CqlEvaluation> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Find all evaluations for a patient with pagination
     */
    Page<CqlEvaluation> findByTenantIdAndPatientId(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find evaluations for a patient ordered by date (most recent first)
     */
    List<CqlEvaluation> findByTenantIdAndPatientIdOrderByEvaluationDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find evaluations by library
     */
    List<CqlEvaluation> findByTenantIdAndLibrary_Id(String tenantId, UUID libraryId);

    /**
     * Find evaluations by library with pagination
     */
    Page<CqlEvaluation> findByTenantIdAndLibrary_Id(
            String tenantId, UUID libraryId, Pageable pageable);

    /**
     * Find evaluations by patient and library
     */
    List<CqlEvaluation> findByTenantIdAndPatientIdAndLibrary_Id(
            String tenantId, UUID patientId, UUID libraryId);

    /**
     * Find the most recent evaluation for a patient and library
     */
    @Query("SELECT e FROM CqlEvaluation e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId AND e.library.id = :libraryId " +
           "ORDER BY e.evaluationDate DESC LIMIT 1")
    Optional<CqlEvaluation> findLatestByPatientAndLibrary(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("libraryId") UUID libraryId);

    /**
     * Find evaluations by status
     */
    List<CqlEvaluation> findByTenantIdAndStatus(String tenantId, String status);

    /**
     * Find evaluations by status with pagination
     */
    Page<CqlEvaluation> findByTenantIdAndStatus(
            String tenantId, String status, Pageable pageable);

    /**
     * Find evaluations by status and library
     */
    List<CqlEvaluation> findByTenantIdAndStatusAndLibrary_Id(
            String tenantId, String status, UUID libraryId);

    /**
     * Find evaluations within a date range
     */
    @Query("SELECT e FROM CqlEvaluation e WHERE e.tenantId = :tenantId " +
           "AND e.evaluationDate BETWEEN :startDate AND :endDate")
    List<CqlEvaluation> findByDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find evaluations for a patient within a date range
     */
    @Query("SELECT e FROM CqlEvaluation e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND e.evaluationDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.evaluationDate DESC")
    List<CqlEvaluation> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find successful evaluations for a patient
     */
    List<CqlEvaluation> findByTenantIdAndPatientIdAndStatus(
            String tenantId, UUID patientId, String status);

    /**
     * Count evaluations by status
     */
    long countByTenantIdAndStatus(String tenantId, String status);

    /**
     * Count evaluations for a library
     */
    long countByTenantIdAndLibrary_Id(String tenantId, UUID libraryId);

    /**
     * Count evaluations for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Get average evaluation duration for a library
     */
    @Query("SELECT AVG(e.durationMs) FROM CqlEvaluation e " +
           "WHERE e.tenantId = :tenantId AND e.library.id = :libraryId " +
           "AND e.status = 'SUCCESS' AND e.durationMs IS NOT NULL")
    Double getAverageDurationForLibrary(
            @Param("tenantId") String tenantId,
            @Param("libraryId") UUID libraryId);

    /**
     * Find failed evaluations that need retry
     */
    @Query("SELECT e FROM CqlEvaluation e WHERE e.tenantId = :tenantId " +
           "AND e.status = 'FAILED' " +
           "AND e.evaluationDate > :cutoffDate " +
           "ORDER BY e.evaluationDate ASC")
    List<CqlEvaluation> findFailedEvaluationsForRetry(
            @Param("tenantId") String tenantId,
            @Param("cutoffDate") Instant cutoffDate);

    /**
     * Delete old evaluations (for data retention policies)
     */
    void deleteByTenantIdAndEvaluationDateBefore(String tenantId, Instant cutoffDate);
}
