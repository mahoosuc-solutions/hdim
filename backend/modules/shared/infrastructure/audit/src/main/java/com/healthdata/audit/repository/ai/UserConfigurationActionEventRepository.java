package com.healthdata.audit.repository.ai;

import com.healthdata.audit.entity.ai.UserConfigurationActionEventEntity;
import com.healthdata.audit.models.ai.UserConfigurationActionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for user configuration action events.
 */
@Repository
public interface UserConfigurationActionEventRepository extends JpaRepository<UserConfigurationActionEventEntity, UUID> {

    /**
     * Find all user actions for a tenant.
     */
    Page<UserConfigurationActionEventEntity> findByTenantIdAndTimestampBetween(
        String tenantId,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find user actions by user ID.
     */
    Page<UserConfigurationActionEventEntity> findByUserIdAndTimestampBetween(
        String userId,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find user actions by action type.
     */
    List<UserConfigurationActionEventEntity> findByActionTypeAndTimestampBetween(
        UserConfigurationActionEvent.ActionType actionType,
        Instant startTime,
        Instant endTime
    );

    /**
     * Find user actions by status.
     */
    List<UserConfigurationActionEventEntity> findByActionStatus(
        UserConfigurationActionEvent.ActionStatus actionStatus
    );

    /**
     * Find user actions linked to an AI recommendation.
     */
    List<UserConfigurationActionEventEntity> findByAiRecommendationId(
        UUID aiRecommendationId
    );

    /**
     * Find user actions by correlation ID.
     */
    List<UserConfigurationActionEventEntity> findByCorrelationIdOrderByTimestampAsc(
        String correlationId
    );

    /**
     * Find user actions that accepted AI recommendations.
     */
    @Query("SELECT e FROM UserConfigurationActionEventEntity e WHERE " +
           "e.aiRecommendationAction = 'ACCEPTED' " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<UserConfigurationActionEventEntity> findAcceptedAIRecommendations(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find user actions that rejected AI recommendations.
     */
    @Query("SELECT e FROM UserConfigurationActionEventEntity e WHERE " +
           "e.aiRecommendationAction = 'REJECTED' " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<UserConfigurationActionEventEntity> findRejectedAIRecommendations(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find user actions with feedback.
     */
    @Query("SELECT e FROM UserConfigurationActionEventEntity e WHERE " +
           "e.userFeedbackRating IS NOT NULL " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<UserConfigurationActionEventEntity> findActionsWithFeedback(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Calculate average feedback rating for AI recommendations.
     */
    @Query("SELECT AVG(e.userFeedbackRating) FROM UserConfigurationActionEventEntity e WHERE " +
           "e.aiRecommendationId IS NOT NULL AND e.userFeedbackRating IS NOT NULL " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    Double calculateAverageAIRecommendationRating(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count user actions by action type.
     */
    @Query("SELECT e.actionType, COUNT(e) FROM UserConfigurationActionEventEntity e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime GROUP BY e.actionType")
    List<Object[]> countActionsByType(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count user actions by source.
     */
    @Query("SELECT e.actionSource, COUNT(e) FROM UserConfigurationActionEventEntity e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime GROUP BY e.actionSource")
    List<Object[]> countActionsBySource(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find failed user actions for troubleshooting.
     */
    @Query("SELECT e FROM UserConfigurationActionEventEntity e WHERE " +
           "e.actionStatus = 'FAILED' " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<UserConfigurationActionEventEntity> findFailedActions(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find actions requiring approval.
     */
    @Query("SELECT e FROM UserConfigurationActionEventEntity e WHERE " +
           "e.approvalStatus = 'PENDING_APPROVAL' " +
           "ORDER BY e.timestamp ASC")
    List<UserConfigurationActionEventEntity> findPendingApprovals();
}
