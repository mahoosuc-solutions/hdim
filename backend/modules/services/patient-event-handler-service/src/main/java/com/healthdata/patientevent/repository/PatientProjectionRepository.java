package com.healthdata.patientevent.repository;

import com.healthdata.patientevent.projection.PatientActiveProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PatientActiveProjection
 *
 * Provides database access to patient projections (denormalized read model).
 * Supports finding patients by ID and tenant for multi-tenant isolation.
 */
@Repository
public interface PatientProjectionRepository extends JpaRepository<PatientActiveProjection, String> {

    /**
     * Find a patient projection by patient ID and tenant ID
     *
     * Multi-tenant isolation: ensures patients from different tenants
     * are never mixed in queries.
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier for isolation
     * @return Patient projection if found, empty otherwise
     */
    Optional<PatientActiveProjection> findByPatientIdAndTenantId(String patientId, String tenantId);

    /**
     * Check if a patient exists for a tenant
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return true if patient exists for this tenant
     */
    boolean existsByPatientIdAndTenantId(String patientId, String tenantId);
}
