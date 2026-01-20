package com.healthdata.clinicalworkflowevent.repository;

import com.healthdata.clinicalworkflowevent.projection.WorkflowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Workflow Projection (CQRS Read Model)
 *
 * Optimized for fast queries on workflow status data.
 * All queries include tenant isolation for multi-tenancy.
 */
@Repository
public interface WorkflowProjectionRepository extends JpaRepository<WorkflowProjection, Long> {

    /**
     * Find workflow by tenant and workflow ID
     */
    Optional<WorkflowProjection> findByTenantIdAndWorkflowId(String tenantId, UUID workflowId);

    /**
     * Find all workflows for a patient
     */
    List<WorkflowProjection> findByTenantIdAndPatientIdOrderByCreatedAtDesc(String tenantId, UUID patientId);

    /**
     * Find pending workflows for a patient
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.patientId = :patientId AND w.status = 'PENDING' ORDER BY w.dueDate ASC")
    List<WorkflowProjection> findPendingForPatient(@Param("tenantId") String tenantId, @Param("patientId") UUID patientId);

    /**
     * Find workflows assigned to a user
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.assignedTo = :assignedTo AND w.status != 'COMPLETED' ORDER BY w.priority DESC, w.dueDate ASC")
    List<WorkflowProjection> findAssignedTo(@Param("tenantId") String tenantId, @Param("assignedTo") String assignedTo);

    /**
     * Find pending workflows assigned to user (paginated)
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.assignedTo = :assignedTo AND w.status = 'PENDING' ORDER BY w.priority DESC, w.dueDate ASC")
    Page<WorkflowProjection> findPendingAssignedTo(@Param("tenantId") String tenantId, @Param("assignedTo") String assignedTo, Pageable pageable);

    /**
     * Find overdue workflows
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.isOverdue = true AND w.status != 'COMPLETED' ORDER BY w.dueDate ASC")
    List<WorkflowProjection> findOverdue(@Param("tenantId") String tenantId);

    /**
     * Find workflows requiring review
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.requiresReview = true ORDER BY w.priority DESC")
    List<WorkflowProjection> findRequiringReview(@Param("tenantId") String tenantId);

    /**
     * Find workflows with blocking issues
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.hasBlockingIssue = true ORDER BY w.priority DESC")
    List<WorkflowProjection> findWithBlockingIssues(@Param("tenantId") String tenantId);

    /**
     * Find workflows by type
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.workflowType = :workflowType AND w.status != 'COMPLETED' ORDER BY w.priority DESC")
    List<WorkflowProjection> findByWorkflowType(@Param("tenantId") String tenantId, @Param("workflowType") String workflowType);

    /**
     * Find workflows by status
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.status = :status ORDER BY w.createdAt DESC")
    Page<WorkflowProjection> findByStatus(@Param("tenantId") String tenantId, @Param("status") String status, Pageable pageable);

    /**
     * Count pending workflows for a tenant
     */
    @Query("SELECT COUNT(w) FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.status = 'PENDING'")
    long countPending(@Param("tenantId") String tenantId);

    /**
     * Count overdue workflows for a tenant
     */
    @Query("SELECT COUNT(w) FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.isOverdue = true AND w.status != 'COMPLETED'")
    long countOverdue(@Param("tenantId") String tenantId);

    /**
     * Count workflows assigned to a user
     */
    @Query("SELECT COUNT(w) FROM WorkflowProjection w WHERE w.tenantId = :tenantId AND w.assignedTo = :assignedTo AND w.status != 'COMPLETED'")
    long countAssignedTo(@Param("tenantId") String tenantId, @Param("assignedTo") String assignedTo);

    /**
     * Get distinct tenant IDs (for rebuild operations)
     */
    @Query("SELECT DISTINCT w.tenantId FROM WorkflowProjection w")
    List<String> findDistinctTenantIds();

    /**
     * Delete all projections for a tenant
     */
    void deleteAllByTenantId(String tenantId);

    /**
     * Delete all projections for a patient
     */
    void deleteAllByTenantIdAndPatientId(String tenantId, UUID patientId);
}
