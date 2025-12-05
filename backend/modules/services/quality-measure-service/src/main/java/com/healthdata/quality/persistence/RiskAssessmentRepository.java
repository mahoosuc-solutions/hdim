package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Risk Assessment operations
 */
@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessmentEntity, UUID> {

    /**
     * Find most recent risk assessment for a patient
     */
    @Query("SELECT r FROM RiskAssessmentEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "ORDER BY r.assessmentDate DESC LIMIT 1")
    Optional<RiskAssessmentEntity> findMostRecent(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId
    );

    /**
     * Find latest risk assessment for a patient (alias for test compatibility)
     */
    default Optional<RiskAssessmentEntity> findLatestByTenantIdAndPatientId(String tenantId, String patientId) {
        return findMostRecent(tenantId, patientId);
    }

    /**
     * Find all risk assessments for a patient ordered by date
     */
    List<RiskAssessmentEntity> findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
        String tenantId,
        String patientId
    );

    /**
     * Find patients by risk level (for population health)
     */
    List<RiskAssessmentEntity> findByTenantIdAndRiskLevelOrderByRiskScoreDesc(
        String tenantId,
        RiskAssessmentEntity.RiskLevel riskLevel
    );

    /**
     * Count patients by risk level
     */
    @Query("SELECT COUNT(DISTINCT r.patientId) FROM RiskAssessmentEntity r " +
           "WHERE r.tenantId = :tenantId AND r.riskLevel = :riskLevel " +
           "AND r.id IN (SELECT MAX(r2.id) FROM RiskAssessmentEntity r2 " +
           "             WHERE r2.tenantId = :tenantId GROUP BY r2.patientId)")
    Long countPatientsByRiskLevel(
        @Param("tenantId") String tenantId,
        @Param("riskLevel") RiskAssessmentEntity.RiskLevel riskLevel
    );

    /**
     * Find latest risk assessment for a specific category and patient (Phase 4)
     */
    @Query("SELECT r FROM RiskAssessmentEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.riskCategory = :category " +
           "ORDER BY r.assessmentDate DESC LIMIT 1")
    Optional<RiskAssessmentEntity> findLatestByCategoryAndPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("category") String category
    );

    /**
     * Find risk assessments for a category within a time period (Phase 4 - Historical tracking)
     */
    @Query("SELECT r FROM RiskAssessmentEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.riskCategory = :category " +
           "AND r.assessmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.assessmentDate ASC")
    List<RiskAssessmentEntity> findByCategoryForPeriod(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("category") String category,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}
