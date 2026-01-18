package com.healthdata.eventsourcing.projection.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for PatientProjection
 *
 * Provides optimized query methods for the patient read model.
 * All queries enforce multi-tenant isolation via tenantId parameter.
 *
 * Key features:
 * - Tenant-scoped queries (all queries filter by tenant_id)
 * - Indexed searches for common patterns (MRN, first name)
 * - Count aggregations for analytics
 * - Efficient pagination support
 */
@Repository
public interface PatientProjectionRepository extends JpaRepository<PatientProjection, UUID> {

    /**
     * Find a patient by tenant and MRN (primary lookup)
     *
     * @param tenantId Tenant identifier
     * @param mrn Medical Record Number
     * @return Optional containing patient if found, empty otherwise
     */
    Optional<PatientProjection> findByTenantIdAndMrn(String tenantId, String mrn);

    /**
     * Find all patients for a specific tenant
     *
     * @param tenantId Tenant identifier
     * @return List of all patients in this tenant
     */
    List<PatientProjection> findByTenantId(String tenantId);

    /**
     * Count total patients for a tenant
     *
     * @param tenantId Tenant identifier
     * @return Number of patients in this tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Search patients by first name prefix for autocomplete
     *
     * @param tenantId Tenant identifier
     * @param firstNamePrefix First name prefix to search
     * @return List of matching patients
     */
    List<PatientProjection> findByTenantIdAndFirstNameStartingWith(String tenantId, String firstNamePrefix);

    /**
     * Find a patient by aggregate ID within tenant
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient aggregate ID
     * @return Optional containing patient if found, empty otherwise
     */
    Optional<PatientProjection> findByTenantIdAndPatientId(String tenantId, String patientId);

    /**
     * Find a patient by patient ID and tenant (alternative parameter order)
     *
     * @param patientId Patient aggregate ID
     * @param tenantId Tenant identifier
     * @return Optional containing patient if found, empty otherwise
     */
    Optional<PatientProjection> findByPatientIdAndTenantId(String patientId, String tenantId);

    /**
     * Find a patient by MRN and tenant (alternative parameter order)
     *
     * @param mrn Medical Record Number
     * @param tenantId Tenant identifier
     * @return Optional containing patient if found, empty otherwise
     */
    Optional<PatientProjection> findByMrnAndTenantId(String mrn, String tenantId);

    /**
     * Find a patient by insurance member ID and tenant
     *
     * @param insuranceMemberId Insurance member ID
     * @param tenantId Tenant identifier
     * @return Optional containing patient if found, empty otherwise
     */
    Optional<PatientProjection> findByInsuranceMemberIdAndTenantId(String insuranceMemberId, String tenantId);
}
