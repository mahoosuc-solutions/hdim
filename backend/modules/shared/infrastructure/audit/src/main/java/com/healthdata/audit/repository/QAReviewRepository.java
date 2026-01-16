package com.healthdata.audit.repository;

import com.healthdata.audit.entity.QAReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * QA Review Repository
 * 
 * Data access for QA review records.
 */
@Repository
public interface QAReviewRepository extends JpaRepository<QAReviewEntity, String> {

    Optional<QAReviewEntity> findByDecisionId(String decisionId);

    List<QAReviewEntity> findByTenantIdAndReviewStatus(String tenantId, String reviewStatus);

    @Query("SELECT r FROM QAReviewEntity r WHERE r.tenantId = :tenantId " +
           "AND r.reviewedAt BETWEEN :start AND :end")
    List<QAReviewEntity> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    // Simplified query - agentType filtering handled in service layer if needed
    // Note: agentType parameter is accepted but filtering is done in service layer
    // to avoid complex JPQL entity joins between qa_reviews and ai_agent_decision_events
    // The agentType parameter is included in WHERE clause to satisfy Spring Data JPA validation
    @Query("SELECT r FROM QAReviewEntity r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.reviewStatus = 'FLAGGED' " +
           "AND (:flagType IS NULL OR r.flagType = :flagType) " +
           "AND (:agentType IS NULL OR :agentType = '' OR 1=1)") // Accept parameter for validation, actual filtering in service
    Page<QAReviewEntity> findFlagged(
            @Param("tenantId") String tenantId,
            @Param("flagType") String flagType,
            @Param("agentType") String agentType, // Parameter kept for API compatibility, filtering done in service layer
            Pageable pageable);
}
