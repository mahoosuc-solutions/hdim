package com.healthdata.audit.repository.clinical;

import com.healthdata.audit.entity.clinical.ClinicalDecisionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for clinical decision entities
 */
@Repository
public interface ClinicalDecisionRepository extends JpaRepository<ClinicalDecisionEntity, UUID> {
    
    @Query("SELECT d FROM ClinicalDecisionEntity d WHERE CAST(d.id AS string) = :decisionId AND d.tenantId = :tenantId")
    Optional<ClinicalDecisionEntity> findByDecisionIdAndTenantId(
        @Param("decisionId") String decisionId,
        @Param("tenantId") String tenantId
    );
    
    @Query("SELECT d FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId " +
           "AND (:decisionType IS NULL OR d.decisionType = :decisionType) " +
           "AND (:alertSeverity IS NULL OR d.alertSeverity = :alertSeverity) " +
           "AND (:reviewStatus IS NULL OR d.reviewStatus = :reviewStatus) " +
           "AND (:startDate IS NULL OR d.decisionTimestamp >= :startDate) " +
           "AND (:endDate IS NULL OR d.decisionTimestamp <= :endDate) " +
           "AND (:evidenceGrade IS NULL OR d.evidenceGrade = :evidenceGrade) " +
           "AND (:hasOverride IS NULL OR d.hasOverride = :hasOverride) " +
           "AND (:specialtyArea IS NULL OR d.specialtyArea = :specialtyArea)")
    Page<ClinicalDecisionEntity> findDecisionHistory(
        @Param("tenantId") String tenantId,
        @Param("decisionType") String decisionType,
        @Param("alertSeverity") String alertSeverity,
        @Param("reviewStatus") String reviewStatus,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("evidenceGrade") String evidenceGrade,
        @Param("hasOverride") Boolean hasOverride,
        @Param("specialtyArea") String specialtyArea,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(d) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.reviewStatus = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);
    
    @Query("SELECT COUNT(d) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.decisionType = :type")
    Long countByTenantIdAndDecisionType(@Param("tenantId") String tenantId, @Param("type") String type);
    
    @Query("SELECT COUNT(d) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.alertSeverity = :severity")
    Long countByTenantIdAndSeverity(@Param("tenantId") String tenantId, @Param("severity") String severity);
    
    @Query("SELECT COUNT(d) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.evidenceGrade = :grade")
    Long countByTenantIdAndEvidenceGrade(@Param("tenantId") String tenantId, @Param("grade") String grade);
    
    @Query("SELECT AVG(d.confidenceScore) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId")
    Double getAverageConfidenceScore(@Param("tenantId") String tenantId);
    
    @Query("SELECT d FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId " +
           "AND d.decisionTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY d.decisionTimestamp ASC")
    Page<ClinicalDecisionEntity> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    @Query("SELECT COUNT(d) FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.hasOverride = true")
    Long countOverridesForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (d.reviewedAt - d.decisionTimestamp))/3600) " +
           "FROM ClinicalDecisionEntity d WHERE d.tenantId = :tenantId AND d.reviewedAt IS NOT NULL")
    Double getAverageReviewTimeHours(@Param("tenantId") String tenantId);
}
