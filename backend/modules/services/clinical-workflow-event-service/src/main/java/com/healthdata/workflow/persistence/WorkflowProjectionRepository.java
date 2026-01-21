package com.healthdata.workflow.persistence;

import com.healthdata.workflow.projection.WorkflowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Workflow Projection Repository
 *
 * Persistence layer for WorkflowProjection (read model)
 * Enables fast queries of workflow state and history
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface WorkflowProjectionRepository extends JpaRepository<WorkflowProjection, String> {

    /**
     * Find workflow by workflow ID and tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.id = :id AND w.tenantId = :tenantId")
    Optional<WorkflowProjection> findByIdAndTenant(
        @Param("id") String id,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all workflows for a patient by tenant
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.patientId = :patientId AND w.tenantId = :tenantId ORDER BY w.lastUpdated DESC")
    List<WorkflowProjection> findByPatientIdAndTenant(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find workflows by status and tenant
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.status = :status AND w.tenantId = :tenantId ORDER BY w.lastUpdated DESC")
    List<WorkflowProjection> findByStatusAndTenant(
        @Param("status") String status,
        @Param("tenantId") String tenantId
    );

    /**
     * Find workflows assigned to a user in a tenant
     */
    @Query("SELECT w FROM WorkflowProjection w WHERE w.assignedTo = :assignedTo AND w.tenantId = :tenantId AND w.status != 'COMPLETED'")
    List<WorkflowProjection> findActiveWorkflowsByAssignee(
        @Param("assignedTo") String assignedTo,
        @Param("tenantId") String tenantId
    );
}
