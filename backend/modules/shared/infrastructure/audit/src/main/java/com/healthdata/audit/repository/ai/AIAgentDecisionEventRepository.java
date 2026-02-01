package com.healthdata.audit.repository.ai;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
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
 * Repository for AI agent decision events.
 */
@Repository
public interface AIAgentDecisionEventRepository extends JpaRepository<AIAgentDecisionEventEntity, UUID> {

    /**
     * Find all AI decisions for a tenant within a time range.
     */
    Page<AIAgentDecisionEventEntity> findByTenantIdAndTimestampBetween(
        String tenantId,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find AI decisions by agent type.
     */
    Page<AIAgentDecisionEventEntity> findByAgentTypeAndTimestampBetween(
        AIAgentDecisionEvent.AgentType agentType,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find AI decisions by decision type.
     */
    Page<AIAgentDecisionEventEntity> findByDecisionTypeAndTimestampBetween(
        AIAgentDecisionEvent.DecisionType decisionType,
        Instant startTime,
        Instant endTime,
        Pageable pageable
    );

    /**
     * Find AI decisions by outcome.
     */
    List<AIAgentDecisionEventEntity> findByOutcome(
        AIAgentDecisionEvent.DecisionOutcome outcome
    );

    /**
     * Find AI decisions by correlation ID (for decision chains).
     */
    List<AIAgentDecisionEventEntity> findByCorrelationIdOrderByTimestampAsc(
        String correlationId
    );

    /**
     * Find AI decisions with confidence score above threshold.
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e WHERE e.confidenceScore >= :threshold " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<AIAgentDecisionEventEntity> findHighConfidenceDecisions(
        @Param("threshold") Double threshold,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find AI decisions with user feedback.
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e WHERE e.userFeedbackRating IS NOT NULL " +
           "AND e.timestamp BETWEEN :startTime AND :endTime")
    List<AIAgentDecisionEventEntity> findDecisionsWithFeedback(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Calculate average confidence score by agent type.
     */
    @Query("SELECT e.agentType, AVG(e.confidenceScore) FROM AIAgentDecisionEventEntity e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime GROUP BY e.agentType")
    List<Object[]> calculateAverageConfidenceByAgentType(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Count decisions by outcome.
     */
    @Query("SELECT e.outcome, COUNT(e) FROM AIAgentDecisionEventEntity e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime GROUP BY e.outcome")
    List<Object[]> countDecisionsByOutcome(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Calculate total AI cost for a tenant.
     */
    @Query("SELECT SUM(e.costEstimate) FROM AIAgentDecisionEventEntity e " +
           "WHERE e.tenantId = :tenantId AND e.timestamp BETWEEN :startTime AND :endTime")
    Double calculateTotalCostForTenant(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find decisions for QA review queue with filtering.
     * This is an alias method that matches the service layer's expectations.
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND (:agentType IS NULL OR CAST(e.agentType AS string) = :agentType) " +
           "AND (:minConfidence IS NULL OR e.confidenceScore >= :minConfidence) " +
           "AND (:maxConfidence IS NULL OR e.confidenceScore <= :maxConfidence) " +
           "AND (:startDate IS NULL OR e.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR e.timestamp <= :endDate) " +
           "AND (:includeReviewed = true OR NOT EXISTS (" +
           "    SELECT r FROM QAReviewEntity r WHERE r.decisionId = CAST(e.id AS string)" +
           ")) " +
           "ORDER BY e.timestamp DESC")
    Page<AIAgentDecisionEventEntity> findReviewQueue(
        @Param("tenantId") String tenantId,
        @Param("agentType") String agentType,
        @Param("minConfidence") Double minConfidence,
        @Param("maxConfidence") Double maxConfidence,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        @Param("includeReviewed") boolean includeReviewed,
        Pageable pageable
    );

    /**
     * Find decision by decision ID and tenant ID (using UUID id field).
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e WHERE CAST(e.id AS string) = :decisionId AND e.tenantId = :tenantId")
    java.util.Optional<AIAgentDecisionEventEntity> findByDecisionIdAndTenantId(
        @Param("decisionId") String decisionId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find decision by decision ID (using UUID id field).
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e WHERE CAST(e.id AS string) = :decisionId")
    java.util.Optional<AIAgentDecisionEventEntity> findByDecisionId(
        @Param("decisionId") String decisionId
    );

    /**
     * Find decisions by list of decision IDs.
     */
    @Query("SELECT e FROM AIAgentDecisionEventEntity e WHERE CAST(e.id AS string) IN :decisionIds")
    List<AIAgentDecisionEventEntity> findByDecisionIdIn(
        @Param("decisionIds") List<String> decisionIds
    );
    
    // ========== Replay Service Methods ==========
    
    /**
     * Find events by tenant and time range (for replay).
     */
    List<AIAgentDecisionEventEntity> findByTenantIdAndTimestampBetweenOrderByTimestampAsc(
        String tenantId,
        Instant startTime,
        Instant endTime
    );

    /**
     * Find events by tenant since a specific timestamp (for SSE streaming).
     */
    Page<AIAgentDecisionEventEntity> findByTenantIdAndTimestampAfter(
        String tenantId,
        Instant timestamp,
        Pageable pageable
    );

    /**
     * Find events by tenant, decision type, and time range.
     */
    List<AIAgentDecisionEventEntity> findByTenantIdAndDecisionTypeAndTimestampBetweenOrderByTimestampAsc(
        String tenantId,
        AIAgentDecisionEvent.DecisionType decisionType,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Find events by tenant, resource ID (patient), and time range.
     */
    List<AIAgentDecisionEventEntity> findByTenantIdAndResourceIdAndTimestampBetweenOrderByTimestampAsc(
        String tenantId,
        String resourceId,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Find events by tenant, agent type, and time range.
     */
    List<AIAgentDecisionEventEntity> findByTenantIdAndAgentTypeAndTimestampBetweenOrderByTimestampAsc(
        String tenantId,
        AIAgentDecisionEvent.AgentType agentType,
        Instant startTime,
        Instant endTime
    );
    
    /**
     * Find events by tenant and correlation ID.
     */
    List<AIAgentDecisionEventEntity> findByTenantIdAndCorrelationIdOrderByTimestampAsc(
        String tenantId,
        String correlationId
    );
    
    /**
     * Find event by tenant and event ID.
     */
    java.util.Optional<AIAgentDecisionEventEntity> findByTenantIdAndEventId(
        String tenantId,
        UUID eventId
    );
}
