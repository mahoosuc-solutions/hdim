package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.domain.model.PreVisitChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PreVisitChecklistEntity
 *
 * Provides data access for pre-visit checklist management with multi-tenant isolation.
 * Supports appointment type-specific checklists and completion tracking.
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL must be <= 5 minutes
 * - Multi-tenant filtering enforced on all queries
 */
@Repository
@Transactional(readOnly = true)
public interface PreVisitChecklistRepository extends JpaRepository<PreVisitChecklistEntity, UUID> {

    /**
     * Find pre-visit checklist by ID and tenant
     *
     * @param id UUID of the checklist
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the checklist if found
     */
    Optional<PreVisitChecklistEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find checklists by appointment type and tenant
     * Used to retrieve standard checklists for specific visit types
     *
     * @param appointmentType Type of appointment (new-patient, follow-up, procedure-pre, etc.)
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of checklists for the appointment type
     */
    @Query("""
        SELECT c FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.appointmentType = :appointmentType
        ORDER BY c.createdAt DESC
    """)
    List<PreVisitChecklistEntity> findByAppointmentTypeAndTenant(
        @Param("appointmentType") String appointmentType,
        @Param("tenantId") String tenantId
    );

    /**
     * Find incomplete checklists by tenant
     * Returns checklists that are pending or in-progress
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of incomplete checklists
     */
    @Query("""
        SELECT c FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.status IN ('pending', 'in-progress')
        ORDER BY c.createdAt ASC
    """)
    List<PreVisitChecklistEntity> findIncompleteChecklistsByTenant(
        @Param("tenantId") String tenantId
    );

    /**
     * Find checklist by appointment ID and tenant
     * Returns the checklist associated with a specific appointment
     *
     * @param appointmentId FHIR Appointment resource ID
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the checklist if found
     */
    @Query("""
        SELECT c FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.appointmentId = :appointmentId
    """)
    Optional<PreVisitChecklistEntity> findChecklistByAppointmentId(
        @Param("appointmentId") String appointmentId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all checklists for a patient
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return List of all checklists for the patient
     */
    List<PreVisitChecklistEntity> findByTenantIdAndPatientIdOrderByCreatedAtDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Find checklists by status and tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param status Checklist status (pending, in-progress, completed)
     * @return List of checklists with the specified status
     */
    List<PreVisitChecklistEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(
        String tenantId,
        String status
    );

    /**
     * Count incomplete checklists for tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Count of pending and in-progress checklists
     */
    @Query("""
        SELECT COUNT(c) FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.status IN ('pending', 'in-progress')
    """)
    long countIncompleteChecklists(@Param("tenantId") String tenantId);

    /**
     * Find checklists completed by specific staff member
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param completedBy Staff identifier who completed the checklist
     * @return List of checklists completed by the staff member
     */
    @Query("""
        SELECT c FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.completedBy = :completedBy
        AND c.status = 'completed'
        ORDER BY c.completedAt DESC
    """)
    List<PreVisitChecklistEntity> findChecklistsCompletedBy(
        @Param("tenantId") String tenantId,
        @Param("completedBy") String completedBy
    );

    /**
     * Find checklists with low completion percentage
     * Returns checklists that are in-progress but have low completion
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param threshold Minimum completion percentage threshold (e.g., 50.00)
     * @return List of checklists below the threshold
     */
    @Query("""
        SELECT c FROM PreVisitChecklistEntity c
        WHERE c.tenantId = :tenantId
        AND c.status = 'in-progress'
        AND c.completionPercentage < :threshold
        ORDER BY c.createdAt ASC
    """)
    List<PreVisitChecklistEntity> findChecklistsWithLowCompletion(
        @Param("tenantId") String tenantId,
        @Param("threshold") double threshold
    );
}
