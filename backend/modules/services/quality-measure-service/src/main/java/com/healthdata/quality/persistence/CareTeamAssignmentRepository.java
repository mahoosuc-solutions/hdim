package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Care Team Assignments
 *
 * Provides methods for querying patient-specific care team member assignments
 * to enable patient-specific alert routing to actual providers.
 */
@Repository
public interface CareTeamAssignmentRepository extends JpaRepository<CareTeamAssignmentEntity, UUID> {

    /**
     * Find all active care team assignments for a patient
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param active Whether to find active assignments only
     * @return List of care team assignments
     */
    List<CareTeamAssignmentEntity> findByTenantIdAndPatientIdAndActiveOrderByContactPriorityAsc(
        String tenantId,
        UUID patientId,
        boolean active
    );

    /**
     * Find active care team member for specific role
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param role Care team role
     * @param active Whether to find active assignments only
     * @return List of care team members with this role
     */
    List<CareTeamAssignmentEntity> findByTenantIdAndPatientIdAndRoleAndActiveOrderByContactPriorityAsc(
        String tenantId,
        UUID patientId,
        String role,
        boolean active
    );

    /**
     * Find primary care team member for specific role
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param role Care team role
     * @param active Whether to find active assignments only
     * @param isPrimary Whether to find primary assignment only
     * @return Primary care team member
     */
    Optional<CareTeamAssignmentEntity> findFirstByTenantIdAndPatientIdAndRoleAndActiveAndIsPrimaryOrderByContactPriorityAsc(
        String tenantId,
        UUID patientId,
        String role,
        boolean active,
        boolean isPrimary
    );

    /**
     * Find all active care team assignments for a provider
     *
     * @param providerId Provider ID
     * @param active Whether to find active assignments only
     * @return List of care team assignments
     */
    List<CareTeamAssignmentEntity> findByProviderIdAndActive(
        String providerId,
        boolean active
    );

    /**
     * Find active care team assignments effective on a specific date
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param effectiveDate Date to check
     * @param active Whether to find active assignments only
     * @return List of care team assignments
     */
    @Query("""
        SELECT c FROM CareTeamAssignmentEntity c
        WHERE c.tenantId = :tenantId
        AND c.patientId = :patientId
        AND c.active = :active
        AND c.effectiveFrom <= :effectiveDate
        AND (c.effectiveTo IS NULL OR c.effectiveTo >= :effectiveDate)
        ORDER BY c.contactPriority ASC
        """)
    List<CareTeamAssignmentEntity> findActiveAssignmentsOnDate(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("effectiveDate") LocalDate effectiveDate,
        @Param("active") boolean active
    );

    /**
     * Find active care team assignments for specific role effective on a date
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param role Care team role
     * @param effectiveDate Date to check
     * @param active Whether to find active assignments only
     * @return List of care team assignments
     */
    @Query("""
        SELECT c FROM CareTeamAssignmentEntity c
        WHERE c.tenantId = :tenantId
        AND c.patientId = :patientId
        AND c.role = :role
        AND c.active = :active
        AND c.effectiveFrom <= :effectiveDate
        AND (c.effectiveTo IS NULL OR c.effectiveTo >= :effectiveDate)
        ORDER BY c.contactPriority ASC
        """)
    List<CareTeamAssignmentEntity> findActiveAssignmentsByRoleOnDate(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("role") String role,
        @Param("effectiveDate") LocalDate effectiveDate,
        @Param("active") boolean active
    );

    /**
     * Check if a patient has a specific care team role assigned
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param role Care team role
     * @param active Whether to check active assignments only
     * @return True if assignment exists
     */
    boolean existsByTenantIdAndPatientIdAndRoleAndActive(
        String tenantId,
        UUID patientId,
        String role,
        boolean active
    );

    /**
     * Find all patients assigned to a provider in a specific role
     *
     * @param tenantId Tenant ID
     * @param providerId Provider ID
     * @param role Care team role
     * @param active Whether to find active assignments only
     * @return List of care team assignments
     */
    List<CareTeamAssignmentEntity> findByTenantIdAndProviderIdAndRoleAndActive(
        String tenantId,
        String providerId,
        String role,
        boolean active
    );
}
