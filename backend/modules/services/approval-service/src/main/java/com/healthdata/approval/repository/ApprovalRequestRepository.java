package com.healthdata.approval.repository;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.ApprovalStatus;
import com.healthdata.approval.domain.entity.ApprovalRequest.RequestType;
import com.healthdata.approval.domain.entity.ApprovalRequest.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    // Find by tenant
    Page<ApprovalRequest> findByTenantId(String tenantId, Pageable pageable);

    Page<ApprovalRequest> findByTenantIdAndStatus(String tenantId, ApprovalStatus status, Pageable pageable);

    Optional<ApprovalRequest> findByTenantIdAndId(String tenantId, UUID id);

    // Find by assignee
    Page<ApprovalRequest> findByAssignedTo(String assignedTo, Pageable pageable);

    Page<ApprovalRequest> findByAssignedToAndStatus(String assignedTo, ApprovalStatus status, Pageable pageable);

    // Find pending for role
    @Query("SELECT a FROM ApprovalRequest a WHERE a.tenantId = :tenantId " +
           "AND a.status IN ('PENDING', 'ASSIGNED') " +
           "AND (:role IS NULL OR a.assignedRole = :role) " +
           "ORDER BY CASE a.riskLevel " +
           "  WHEN 'CRITICAL' THEN 0 " +
           "  WHEN 'HIGH' THEN 1 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'LOW' THEN 3 END, a.createdAt ASC")
    Page<ApprovalRequest> findPendingByTenantAndRole(
        @Param("tenantId") String tenantId,
        @Param("role") String role,
        Pageable pageable
    );

    // Find by type
    Page<ApprovalRequest> findByTenantIdAndRequestType(
        String tenantId, RequestType requestType, Pageable pageable
    );

    // Find by risk level
    Page<ApprovalRequest> findByTenantIdAndRiskLevel(
        String tenantId, RiskLevel riskLevel, Pageable pageable
    );

    // Find expiring soon for a specific tenant
    @Query("SELECT a FROM ApprovalRequest a WHERE a.tenantId = :tenantId " +
           "AND a.status IN ('PENDING', 'ASSIGNED') " +
           "AND a.expiresAt IS NOT NULL " +
           "AND a.expiresAt <= :threshold " +
           "ORDER BY a.expiresAt ASC")
    List<ApprovalRequest> findExpiringSoon(
        @Param("tenantId") String tenantId,
        @Param("threshold") Instant threshold
    );

    // Find expiring soon across all tenants (for scheduled job)
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status IN ('PENDING', 'ASSIGNED') " +
           "AND a.expiresAt IS NOT NULL " +
           "AND a.expiresAt <= :threshold " +
           "ORDER BY a.expiresAt ASC")
    List<ApprovalRequest> findExpiringSoonAllTenants(@Param("threshold") Instant threshold);

    // Find expired
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status IN ('PENDING', 'ASSIGNED') " +
           "AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<ApprovalRequest> findExpired(@Param("now") Instant now);

    // Expire requests
    @Modifying
    @Query("UPDATE ApprovalRequest a SET a.status = 'EXPIRED', a.updatedAt = :now " +
           "WHERE a.status IN ('PENDING', 'ASSIGNED') " +
           "AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    int expireRequests(@Param("now") Instant now);

    // Count by status
    long countByTenantIdAndStatus(String tenantId, ApprovalStatus status);

    // Count pending by assignee
    long countByAssignedToAndStatusIn(String assignedTo, List<ApprovalStatus> statuses);

    // Find by correlation ID (for linking related requests)
    List<ApprovalRequest> findByTenantIdAndCorrelationId(String tenantId, String correlationId);

    // Statistics queries
    @Query("SELECT a.requestType, COUNT(a) FROM ApprovalRequest a " +
           "WHERE a.tenantId = :tenantId AND a.createdAt >= :since " +
           "GROUP BY a.requestType")
    List<Object[]> countByTypeAndTenant(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    @Query("SELECT a.riskLevel, COUNT(a) FROM ApprovalRequest a " +
           "WHERE a.tenantId = :tenantId AND a.status IN ('PENDING', 'ASSIGNED') " +
           "GROUP BY a.riskLevel")
    List<Object[]> countPendingByRiskLevel(@Param("tenantId") String tenantId);

    @Query("SELECT AVG(EXTRACT(EPOCH FROM (a.decisionAt - a.createdAt))) FROM ApprovalRequest a " +
           "WHERE a.tenantId = :tenantId AND a.status IN ('APPROVED', 'REJECTED') " +
           "AND a.createdAt >= :since")
    Double averageDecisionTimeSeconds(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    // Find stale assigned requests (for auto-escalation)
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = 'ASSIGNED' " +
           "AND a.assignedAt IS NOT NULL " +
           "AND a.assignedAt < :threshold " +
           "ORDER BY a.assignedAt ASC")
    List<ApprovalRequest> findStaleAssignedRequests(@Param("threshold") Instant threshold);

    // Find unassigned pending requests (for auto-assignment)
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = 'PENDING' " +
           "AND a.assignedTo IS NULL " +
           "ORDER BY CASE a.riskLevel " +
           "  WHEN 'CRITICAL' THEN 0 " +
           "  WHEN 'HIGH' THEN 1 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'LOW' THEN 3 END, a.createdAt ASC")
    List<ApprovalRequest> findUnassignedPendingRequests();

    // Find unassigned pending requests for a specific tenant
    @Query("SELECT a FROM ApprovalRequest a WHERE a.tenantId = :tenantId " +
           "AND a.status = 'PENDING' AND a.assignedTo IS NULL " +
           "ORDER BY CASE a.riskLevel " +
           "  WHEN 'CRITICAL' THEN 0 " +
           "  WHEN 'HIGH' THEN 1 " +
           "  WHEN 'MEDIUM' THEN 2 " +
           "  WHEN 'LOW' THEN 3 END, a.createdAt ASC")
    List<ApprovalRequest> findUnassignedPendingRequestsByTenant(@Param("tenantId") String tenantId);
}
