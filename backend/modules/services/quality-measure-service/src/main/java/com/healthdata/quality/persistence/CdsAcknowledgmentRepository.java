package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for CDS Acknowledgment operations
 */
@Repository
public interface CdsAcknowledgmentRepository extends JpaRepository<CdsAcknowledgmentEntity, UUID> {

    /**
     * Find all acknowledgments for a recommendation
     */
    List<CdsAcknowledgmentEntity> findByRecommendationIdOrderByCreatedAtDesc(UUID recommendationId);

    /**
     * Find acknowledgments by patient
     */
    List<CdsAcknowledgmentEntity> findByTenantIdAndPatientIdOrderByCreatedAtDesc(
        String tenantId,
        String patientId
    );

    /**
     * Find acknowledgments by user
     */
    List<CdsAcknowledgmentEntity> findByTenantIdAndUserIdOrderByCreatedAtDesc(
        String tenantId,
        String userId
    );

    /**
     * Find acknowledgments by action type
     */
    List<CdsAcknowledgmentEntity> findByTenantIdAndActionTypeOrderByCreatedAtDesc(
        String tenantId,
        CdsAcknowledgmentEntity.ActionType actionType
    );

    /**
     * Find recent acknowledgments for a recommendation
     */
    @Query("SELECT a FROM CdsAcknowledgmentEntity a " +
           "WHERE a.recommendationId = :recommendationId " +
           "ORDER BY a.createdAt DESC")
    List<CdsAcknowledgmentEntity> findRecentByRecommendation(
        @Param("recommendationId") UUID recommendationId
    );

    /**
     * Find acknowledgments within a time range
     */
    @Query("SELECT a FROM CdsAcknowledgmentEntity a " +
           "WHERE a.tenantId = :tenantId " +
           "AND a.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY a.createdAt DESC")
    List<CdsAcknowledgmentEntity> findByTimeRange(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count acknowledgments by action type for a patient
     */
    @Query("SELECT COUNT(a) FROM CdsAcknowledgmentEntity a " +
           "WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.actionType = :actionType")
    Long countByPatientAndAction(
        @Param("tenantId") String tenantId,
        @Param("patientId") String patientId,
        @Param("actionType") CdsAcknowledgmentEntity.ActionType actionType
    );

    /**
     * Check if a user has acknowledged a recommendation
     */
    @Query("SELECT COUNT(a) > 0 FROM CdsAcknowledgmentEntity a " +
           "WHERE a.recommendationId = :recommendationId " +
           "AND a.userId = :userId " +
           "AND a.actionType = 'ACKNOWLEDGED'")
    boolean hasUserAcknowledged(
        @Param("recommendationId") UUID recommendationId,
        @Param("userId") String userId
    );

    /**
     * Get audit trail summary for a recommendation
     */
    @Query("SELECT a.actionType, COUNT(a) FROM CdsAcknowledgmentEntity a " +
           "WHERE a.recommendationId = :recommendationId " +
           "GROUP BY a.actionType")
    List<Object[]> getActionSummary(@Param("recommendationId") UUID recommendationId);
}
