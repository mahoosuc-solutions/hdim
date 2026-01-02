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
 * Repository for CDS Recommendation operations
 */
@Repository
public interface CdsRecommendationRepository extends JpaRepository<CdsRecommendationEntity, UUID> {

    /**
     * Find all active recommendations for a patient, ordered by urgency and priority
     */
    @Query("SELECT r FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS') " +
           "ORDER BY CASE r.urgency " +
           "  WHEN 'EMERGENT' THEN 0 " +
           "  WHEN 'URGENT' THEN 1 " +
           "  WHEN 'SOON' THEN 2 " +
           "  WHEN 'ROUTINE' THEN 3 END, " +
           "r.priority ASC")
    List<CdsRecommendationEntity> findActiveRecommendations(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find recommendations by status
     */
    List<CdsRecommendationEntity> findByTenantIdAndPatientIdAndStatusOrderByUrgencyAscPriorityAsc(
        String tenantId,
        UUID patientId,
        CdsRecommendationEntity.CdsStatus status
    );

    /**
     * Find recommendations by category
     */
    List<CdsRecommendationEntity> findByTenantIdAndPatientIdAndCategoryOrderByUrgencyAscPriorityAsc(
        String tenantId,
        UUID patientId,
        CdsRuleEntity.CdsCategory category
    );

    /**
     * Find urgent and emergent recommendations for a patient
     */
    @Query("SELECT r FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED') " +
           "AND r.urgency IN ('EMERGENT', 'URGENT') " +
           "ORDER BY r.urgency ASC, r.priority ASC")
    List<CdsRecommendationEntity> findUrgentRecommendations(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Count active recommendations by urgency
     */
    @Query("SELECT COUNT(r) FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS') " +
           "AND r.urgency = :urgency")
    Long countActiveByUrgency(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("urgency") CdsRuleEntity.CdsUrgency urgency
    );

    /**
     * Count all active recommendations for a patient
     */
    @Query("SELECT COUNT(r) FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS')")
    Long countActiveRecommendations(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find overdue recommendations
     */
    @Query("SELECT r FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS') " +
           "AND r.dueDate < :now " +
           "ORDER BY r.dueDate ASC")
    List<CdsRecommendationEntity> findOverdueRecommendations(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("now") Instant now
    );

    /**
     * Find recommendations by rule
     */
    List<CdsRecommendationEntity> findByTenantIdAndRuleIdAndStatusOrderByCreatedAtDesc(
        String tenantId,
        UUID ruleId,
        CdsRecommendationEntity.CdsStatus status
    );

    /**
     * Check if an active recommendation already exists for a patient and rule
     */
    @Query("SELECT COUNT(r) > 0 FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "AND r.ruleId = :ruleId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS')")
    boolean existsActiveRecommendation(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("ruleId") UUID ruleId
    );

    /**
     * Find recommendations related to a care gap
     */
    List<CdsRecommendationEntity> findByTenantIdAndRelatedCareGapIdOrderByUrgencyAscPriorityAsc(
        String tenantId,
        UUID relatedCareGapId
    );

    /**
     * Find all recommendations for dashboard (paginated)
     */
    @Query("SELECT r FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS') " +
           "ORDER BY r.urgency ASC, r.priority ASC, r.dueDate ASC")
    Page<CdsRecommendationEntity> findAllActiveForDashboard(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find recommendations by multiple filters
     */
    @Query("SELECT r FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId " +
           "AND (:patientId IS NULL OR r.patientId = :patientId) " +
           "AND (:category IS NULL OR r.category = :category) " +
           "AND (:urgency IS NULL OR r.urgency = :urgency) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.urgency ASC, r.priority ASC")
    List<CdsRecommendationEntity> findByFilters(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("category") CdsRuleEntity.CdsCategory category,
        @Param("urgency") CdsRuleEntity.CdsUrgency urgency,
        @Param("status") CdsRecommendationEntity.CdsStatus status
    );

    /**
     * Get statistics by status
     */
    @Query("SELECT r.status, COUNT(r) FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId AND r.patientId = :patientId " +
           "GROUP BY r.status")
    List<Object[]> getStatusCounts(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Get statistics by category
     */
    @Query("SELECT r.category, COUNT(r) FROM CdsRecommendationEntity r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.status IN ('ACTIVE', 'ACKNOWLEDGED', 'IN_PROGRESS') " +
           "GROUP BY r.category")
    List<Object[]> getActiveCategoryCounts(
        @Param("tenantId") String tenantId
    );
}
