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

    @Query("SELECT r FROM QAReviewEntity r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.reviewStatus = 'FLAGGED' " +
           "AND (:flagType IS NULL OR r.flagType = :flagType) " +
           "AND (:agentType IS NULL OR EXISTS (" +
           "    SELECT e FROM AIAuditEvent e WHERE e.decisionId = r.decisionId AND e.agentType = :agentType" +
           "))")
    Page<QAReviewEntity> findFlagged(
            @Param("tenantId") String tenantId,
            @Param("flagType") String flagType,
            @Param("agentType") String agentType,
            Pageable pageable);
}
